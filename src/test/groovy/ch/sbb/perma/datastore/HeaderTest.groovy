/*
 * Copyright (C) Schweizerische Bundesbahnen SBB, 2017.
 */

package ch.sbb.perma.datastore

import spock.lang.Specification

class HeaderTest extends Specification {

    def writeReadFull() {
        given:
        def out = new ByteArrayOutputStream()

        when:
        def newHeader = Header.newFullHeader("foo",1)
        newHeader.writeTo(out)
        def reread = Header.readFrom(new ByteArrayInputStream(out.toByteArray()))

        then:
        reread.isFullFile()
        newHeader.belongsToSameFullFileAs(newHeader)
    }

    def writeReadDelta() {
        given:
        def out = new ByteArrayOutputStream()

        when:
        def newHeader = Header.newFullHeader("foo",42)
        newHeader.nextDelta(1).writeTo(out)
        def reread = Header.readFrom(new ByteArrayInputStream(out.toByteArray()))

        then:
        reread.isNextDeltaFileOf(newHeader)
    }

    def write2ReadDelta() {
        given:
        def out = new ByteArrayOutputStream()

        when:
        def newHeader = Header.newFullHeader("foo",2)
        newHeader.nextDelta(3).nextDelta(5).writeTo(out)
        def reread = Header.readFrom(new ByteArrayInputStream(out.toByteArray()))

        then:
        !reread.isNextDeltaFileOf(newHeader)
        reread.belongsToSameFullFileAs(newHeader)
    }

    def invalidMarker() {
        given:
        def out = new ByteArrayOutputStream()

        when:
        Header.newFullHeader("foo",7).writeTo(out)
        def bytes = out.toByteArray()
        bytes[0] = 0x00
        Header.readFrom(new ByteArrayInputStream(bytes))

        then:
        thrown InvalidDataException
    }

    def brokenCrc() {
        given:
        def out = new ByteArrayOutputStream()

        when:
        Header.newFullHeader("foo",7).writeTo(out)
        def bytes = out.toByteArray()
        bytes[10] = 0xF9
        Header.readFrom(new ByteArrayInputStream(bytes))

        then:
        thrown InvalidDataException
    }

    def toStringIsImplemented() {
        when:
        def headerToString = Header.newFullHeader("foo",7).toString()

        then:
        !headerToString.contains('@')
    }
}

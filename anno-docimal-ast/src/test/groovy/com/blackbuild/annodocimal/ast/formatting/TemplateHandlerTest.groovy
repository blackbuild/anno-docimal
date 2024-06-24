/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2024 Stephan Pauxberger
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.blackbuild.annodocimal.ast.formatting

import spock.lang.Specification

class TemplateHandlerTest extends Specification {

    def "text ist replaced correctly"() {
        given:
        def template = "Hello {{name}}."

        when:
        def result = TemplateHandler.renderTemplates(template, [name: "World"], [])

        then:
        result == "Hello World."
    }

    def "text ist replaced correctly with multiple templates"() {
        given:
        def template = "Hello {{name}}. My name is {{myName}}."

        when:
        def result = TemplateHandler.renderTemplates(template, [name: "World", myName: "Docimal"], [])

        then:
        result == "Hello World. My name is Docimal."
    }

    def "same variable is replaced multiple times"() {
        given:
        def template = "Hello {{name}}. My name is {{name}}."

        when:
        def result = TemplateHandler.renderTemplates(template, [name: "World"], [])

        then:
        result == "Hello World. My name is World."
    }

    def "missing templates are replaced with template key"() {
        given:
        def template = "Hello {{name}}. My name is {{myName}}."

        when:
        def result = TemplateHandler.renderTemplates(template, [name: "World"], [])

        then:
        result == "Hello World. My name is myName."
    }

    def "missing templates are replaced with template default value"() {
        given:
        def template = "Hello {{name}}. My name is {{myName:unknown}}."

        when:
        def result = TemplateHandler.renderTemplates(template, [name: "World"], [])

        then:
        result == "Hello World. My name is unknown."
    }

    def "params are replaced if param is present"() {
        given:
        def template = "Hello World. {{param:name?A name is given.}}"

        when:
        def result = TemplateHandler.renderTemplates(template, [:], ["name"])

        then:
        result == "Hello World. A name is given."
    }

    def "params are removed if param is not present"() {
        given:
        def template = "Hello World. {{param:name?A name is given.}}"

        when:
        def result = TemplateHandler.renderTemplates(template, [:], [])

        then:
        result == "Hello World."
    }

    def "params are removed if param is not present, leading and trailing space are coalesced"() {
        given:
        def template = "Hello World {{param:handshake?using handshake}} with a wink."

        when:
        def result = TemplateHandler.renderTemplates(template, [:], [])

        then:
        result == "Hello World with a wink."
    }

    def "else part of param is used if param is not present"() {
        given:
        def template = "Hello World. {{param:name?A name is given.:Default name is used.}}"

        when:
        def result = TemplateHandler.renderTemplates(template, [:], [])

        then:
        result == "Hello World. Default name is used."
    }
// template conditional

    def "template conditionals are replaced if value is present"() {
        given:
        def template = "Hello World. {{name?A name is given.}}"

        when:
        def result = TemplateHandler.renderTemplates(template, [name: "bla"], [])

        then:
        result == "Hello World. A name is given."
    }

    def "template conditionals are removed if value is not present"() {
        given:
        def template = "Hello World. {{name?A name is given.}}"

        when:
        def result = TemplateHandler.renderTemplates(template, [:], [])

        then:
        result == "Hello World."
    }

    def "template conditionals are removed if value is not present, leading and trailing space are coalesced"() {
        given:
        def template = "Hello World {{handshake?using handshake}} with a wink."

        when:
        def result = TemplateHandler.renderTemplates(template, [:], [])

        then:
        result == "Hello World with a wink."
    }

    def "else part of template conditional is used if value is not present"() {
        given:
        def template = "Hello World. {{name?A name is given.:Default name is used.}}"

        when:
        def result = TemplateHandler.renderTemplates(template, [:], [])

        then:
        result == "Hello World. Default name is used."
    }
}

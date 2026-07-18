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

# baseline
method com.blackbuild.annodocimal.ast.AstDocumentation#attach(org.codehaus.groovy.ast.AnnotatedNode,com.blackbuild.annodocimal.ast.Documentation):void
method com.blackbuild.annodocimal.ast.AstDocumentation#attachText(org.codehaus.groovy.ast.AnnotatedNode,java.lang.String):void
method com.blackbuild.annodocimal.ast.AstDocumentation#extractExact(org.codehaus.groovy.ast.AnnotatedNode):java.util.Optional
method com.blackbuild.annodocimal.ast.AstDocumentation#referenceTo(org.codehaus.groovy.ast.AnnotatedNode):com.blackbuild.annodocimal.ast.Documentation$Link
method com.blackbuild.annodocimal.ast.Documentation#builder():com.blackbuild.annodocimal.ast.Documentation$Builder
method com.blackbuild.annodocimal.ast.Documentation#empty():com.blackbuild.annodocimal.ast.Documentation
method com.blackbuild.annodocimal.ast.Documentation#equals(java.lang.Object):boolean
method com.blackbuild.annodocimal.ast.Documentation#getBlocks():java.util.List
method com.blackbuild.annodocimal.ast.Documentation#getExceptions():java.util.Map
method com.blackbuild.annodocimal.ast.Documentation#getParameters():java.util.Map
method com.blackbuild.annodocimal.ast.Documentation#getReturnDescription():java.lang.String
method com.blackbuild.annodocimal.ast.Documentation#getSummary():java.lang.String
method com.blackbuild.annodocimal.ast.Documentation#getTags():java.util.List
method com.blackbuild.annodocimal.ast.Documentation#getTemplateValues():java.util.Map
method com.blackbuild.annodocimal.ast.Documentation#hashCode():int
method com.blackbuild.annodocimal.ast.Documentation#isEmpty():boolean
method com.blackbuild.annodocimal.ast.Documentation#parse(java.lang.String):com.blackbuild.annodocimal.ast.Documentation
method com.blackbuild.annodocimal.ast.Documentation#render():java.lang.String
method com.blackbuild.annodocimal.ast.Documentation#render(java.util.Collection):java.lang.String
method com.blackbuild.annodocimal.ast.Documentation#toBuilder():com.blackbuild.annodocimal.ast.Documentation$Builder
method com.blackbuild.annodocimal.ast.Documentation$Block#equals(java.lang.Object):boolean
method com.blackbuild.annodocimal.ast.Documentation$Block#getText():java.lang.String
method com.blackbuild.annodocimal.ast.Documentation$Block#hashCode():int
method com.blackbuild.annodocimal.ast.Documentation$Block#isCode():boolean
method com.blackbuild.annodocimal.ast.Documentation$Builder#append(com.blackbuild.annodocimal.ast.Documentation):com.blackbuild.annodocimal.ast.Documentation$Builder
method com.blackbuild.annodocimal.ast.Documentation$Builder#appendParagraph(java.lang.String):com.blackbuild.annodocimal.ast.Documentation$Builder
method com.blackbuild.annodocimal.ast.Documentation$Builder#build():com.blackbuild.annodocimal.ast.Documentation
method com.blackbuild.annodocimal.ast.Documentation$Builder#codeBlock(java.lang.String):com.blackbuild.annodocimal.ast.Documentation$Builder
method com.blackbuild.annodocimal.ast.Documentation$Builder#deprecated(java.lang.String):com.blackbuild.annodocimal.ast.Documentation$Builder
method com.blackbuild.annodocimal.ast.Documentation$Builder#filterParameters(java.util.Collection):com.blackbuild.annodocimal.ast.Documentation$Builder
method com.blackbuild.annodocimal.ast.Documentation$Builder#paragraph(java.lang.String):com.blackbuild.annodocimal.ast.Documentation$Builder
method com.blackbuild.annodocimal.ast.Documentation$Builder#param(java.lang.String,java.lang.String):com.blackbuild.annodocimal.ast.Documentation$Builder
method com.blackbuild.annodocimal.ast.Documentation$Builder#replace(com.blackbuild.annodocimal.ast.Documentation):com.blackbuild.annodocimal.ast.Documentation$Builder
method com.blackbuild.annodocimal.ast.Documentation$Builder#replaceBlocks(java.util.Collection):com.blackbuild.annodocimal.ast.Documentation$Builder
method com.blackbuild.annodocimal.ast.Documentation$Builder#replaceExceptions(java.util.Map):com.blackbuild.annodocimal.ast.Documentation$Builder
method com.blackbuild.annodocimal.ast.Documentation$Builder#replaceParameters(java.util.Map):com.blackbuild.annodocimal.ast.Documentation$Builder
method com.blackbuild.annodocimal.ast.Documentation$Builder#replaceReturn(java.lang.String):com.blackbuild.annodocimal.ast.Documentation$Builder
method com.blackbuild.annodocimal.ast.Documentation$Builder#replaceTags(java.util.Collection):com.blackbuild.annodocimal.ast.Documentation$Builder
method com.blackbuild.annodocimal.ast.Documentation$Builder#returns(java.lang.String):com.blackbuild.annodocimal.ast.Documentation$Builder
method com.blackbuild.annodocimal.ast.Documentation$Builder#see(com.blackbuild.annodocimal.ast.Documentation$Link):com.blackbuild.annodocimal.ast.Documentation$Builder
method com.blackbuild.annodocimal.ast.Documentation$Builder#seeText(java.lang.String):com.blackbuild.annodocimal.ast.Documentation$Builder
method com.blackbuild.annodocimal.ast.Documentation$Builder#summary(java.lang.String):com.blackbuild.annodocimal.ast.Documentation$Builder
method com.blackbuild.annodocimal.ast.Documentation$Builder#tag(java.lang.String,java.lang.String):com.blackbuild.annodocimal.ast.Documentation$Builder
method com.blackbuild.annodocimal.ast.Documentation$Builder#template(java.lang.String,java.lang.String):com.blackbuild.annodocimal.ast.Documentation$Builder
method com.blackbuild.annodocimal.ast.Documentation$Builder#templateValues(java.util.Map):com.blackbuild.annodocimal.ast.Documentation$Builder
method com.blackbuild.annodocimal.ast.Documentation$Builder#throwsException(java.lang.String,java.lang.String):com.blackbuild.annodocimal.ast.Documentation$Builder
method com.blackbuild.annodocimal.ast.Documentation$Link#getLabel():java.lang.String
method com.blackbuild.annodocimal.ast.Documentation$Link#getTarget():java.lang.String
method com.blackbuild.annodocimal.ast.Documentation$Link#inline():java.lang.String
method com.blackbuild.annodocimal.ast.Documentation$Link#text(java.lang.String):com.blackbuild.annodocimal.ast.Documentation$Link
method com.blackbuild.annodocimal.ast.Documentation$Link#text(java.lang.String,java.lang.String):com.blackbuild.annodocimal.ast.Documentation$Link
method com.blackbuild.annodocimal.ast.Documentation$Tag#equals(java.lang.Object):boolean
method com.blackbuild.annodocimal.ast.Documentation$Tag#getName():java.lang.String
method com.blackbuild.annodocimal.ast.Documentation$Tag#getValue():java.lang.String
method com.blackbuild.annodocimal.ast.Documentation$Tag#hashCode():int
type com.blackbuild.annodocimal.ast.AstDocumentation
type com.blackbuild.annodocimal.ast.Documentation
type com.blackbuild.annodocimal.ast.Documentation$Block
type com.blackbuild.annodocimal.ast.Documentation$Builder
type com.blackbuild.annodocimal.ast.Documentation$Link
type com.blackbuild.annodocimal.ast.Documentation$Tag
type com.blackbuild.annodocimal.ast.Documentation$TemplateException

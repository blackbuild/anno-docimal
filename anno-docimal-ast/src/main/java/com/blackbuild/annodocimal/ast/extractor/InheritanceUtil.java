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
package com.blackbuild.annodocimal.ast.extractor;

import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.MethodNode;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Utility class for working with inheritance hierarchies.
 */
public class InheritanceUtil {

    private InheritanceUtil() {
        // Utility class
    }

    /**
     * Finds the first class in the hierarchy of the given node that matches the given predicate. This is a breadth-first
     * search, with superclasses being visited before interfaces.
     *
     * @param node      the node to start the search from
     * @param predicate the predicate to match
     * @return the first class in the hierarchy that matches the predicate
     */
    public static Optional<ClassNode> findMatchingClass(ClassNode node, Predicate<ClassNode> predicate) {
        return getHierarchyStream(node)
                .filter(predicate)
                .findFirst();
    }

    /**
     * Finds the first non-null conversion result in the hierarchy of the given classnode. This is a breadth-first
     * search, with superclasses being visited before interfaces.
     * @param node the node to start the search from
     * @param converter the converter to apply to each class node
     * @return the first non-null conversion result in the hierarchy
     * @param <T> the type of the conversion result
     */
    public static <T> Optional<T> findClassSearchResult(ClassNode node, Function<ClassNode, T> converter) {
        return getHierarchyStream(node)
                .map(converter)
                .filter(Objects::nonNull)
                .findFirst();
    }

    /**
     * Returns the full hierarchy of the given class node, starting with the class itself. This is a breadth-first
     * search, with superclasses being visited before interfaces.
     * @param node the node to start the search from
     * @return the full hierarchy of the class node
     */
    public static List<ClassNode> getClassHierarchy(ClassNode node) {
        return getHierarchyStream(node).collect(Collectors.toList());
    }

    /**
     * Returns the full hierarchy of the given class node, starting with the class itself. This is a breadth-first
     * search, with superclasses being visited before interfaces.
     * @param node the node to start the search from
     * @return the full hierarchy of the class node
     */
    public static Stream<ClassNode> getHierarchyStream(ClassNode node) {
        return StreamSupport.stream(Spliterators.spliteratorUnknownSize(new HierarchyIterator(node), Spliterator.ORDERED), false);
    }

    /**
     * Returns the full hierarchy of the given class node, excluding the class itself. This is a breadth-first
     * search, with superclasses being visited before interfaces.
     * @param node the node to start the search from
     * @return the full hierarchy of the class node
     */
    public static Stream<ClassNode> getAncestorStream(ClassNode node) {
        HierarchyIterator iterator = new HierarchyIterator(node);
        iterator.next();
        return StreamSupport.stream(Spliterators.spliteratorUnknownSize(iterator, Spliterator.ORDERED), false);
    }

    static class HierarchyIterator implements Iterator<ClassNode> {
        private final Deque<ClassNode> toVisit = new ArrayDeque<>();
        private final Set<ClassNode> visited = new HashSet<>();

        public HierarchyIterator(ClassNode node) {
            toVisit.add(node);
        }

        @Override
        public boolean hasNext() {
            return !toVisit.isEmpty();
        }

        @Override
        public ClassNode next() {
            if (!hasNext()) throw new NoSuchElementException();
            ClassNode current = toVisit.removeFirst();
            visited.add(current);
            addToVisit(current.getSuperClass());
            Arrays.stream(current.getInterfaces()).forEach(this::addToVisit);
            return current;
        }

        private void addToVisit(ClassNode node) {
            if (node == null) return;
            if (visited.add(node)) toVisit.add(node);
        }
    }

    /**
     * Returns the full hierarchy of the given method node, starting with the method itself. I.e.
     * this returns the method as well as all methods that this method overrides or implements. This is a breadth-first
     * search, with superclasses being visited before interfaces.
     * @param node the node to start the search from
     * @return the full hierarchy of the method node
     */
    public static Stream<MethodNode> getMethodHierarchy(MethodNode node) {
        if (!node.isPublic() && !node.isProtected()) return Stream.of(node);
        return getHierarchyStream(node.getDeclaringClass())
                .map(c -> c.getDeclaredMethod(node.getName(), node.getParameters()))
                .filter(Objects::nonNull);
    }

    /**
     * Returns the full hierarchy of the given method node, excluding the method itself. I.e.
     * this return all methods that this method overrides or implements. This is a breadth-first
     * search, with superclasses being visited before interfaces.
     * @param node the node to start the search from
     * @return the full hierarchy of the method node
     */
    public static Stream<MethodNode> getSuperMethods(MethodNode node) {
        if (!node.isPublic() && !node.isProtected()) return Stream.empty();
        return getAncestorStream(node.getDeclaringClass())
                .map(c -> c.getDeclaredMethod(node.getName(), node.getParameters()))
                .filter(Objects::nonNull);
    }
}

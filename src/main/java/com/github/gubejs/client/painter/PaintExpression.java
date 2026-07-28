/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Gubejs - KubeJS-compatible scripting for Minecraft, on GraalJS
 * Copyright (C) 2026 xinian and Gubejs contributors
 *
 * This program is free software: you can redistribute it and/or modify it under the terms
 * of the GNU Lesser General Public License, version 3, as published by the Free Software
 * Foundation.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with this
 * program. If not, see <https://www.gnu.org/licenses/>.
 */
package com.github.gubejs.client.painter;

import com.github.gubejs.util.ConsoleJS;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.DoubleBinaryOperator;

/**
 * A number in a description that is written as a sum rather than as a value.
 *
 * <pre>{@code
 * { type: 'text', text: 'centred', x: '$SW/2', y: '$SH-30' }
 * { type: 'rectangle', x: '10%', y: 4, w: '80%', h: 6 }
 * { type: 'item', item: 'minecraft:clock', x: '$SW-$W-10', y: 10 }
 * }</pre>
 *
 * <p>Worked out per frame, because the window can be resized between two of them, and parsed once,
 * because a description is drawn sixty times a second and the string it was written as does not
 * change. That split is the whole reason this is an object and not a static method.
 *
 * <p>A source that makes no sense is complained about once and then read as {@code 0}: a HUD
 * element quietly sitting in the top-left corner is the one outcome a pack author cannot debug.
 */
final class PaintExpression {

    /** A size that is not known yet, for the pass in which {@code w} and {@code h} are read. */
    static final int UNKNOWN = Integer.MIN_VALUE;

    private static final Node ZERO = (frame, percentOf) -> 0D;

    private static final int REPORT_LIMIT = 64;

    /**
     * What has already been complained about.
     *
     * <p>Shared by every expression, and not a flag on each one, because a script drawing through
     * {@code ClientEvents.paintScreen} describes its objects afresh every frame — the same broken
     * source would otherwise be sixty identical lines a second. Capped, so a script building its
     * sources by hand out of changing numbers cannot grow this without bound.
     */
    private static final Set<String> REPORTED = ConcurrentHashMap.newKeySet();

    private final String source;

    private final Node root;

    private PaintExpression(String source) {
        this.source = source;
        var parsed = ZERO;

        try {
            parsed = new Parser(source).parse();
        } catch (IllegalArgumentException failure) {
            report(failure.getMessage());
        }

        this.root = parsed;
    }

    /**
     * Parses one, complaining now if it cannot be parsed at all.
     *
     * @param source what the description said, such as {@code $SW/2-50} or {@code 25%}
     * @return an expression, which reads as {@code 0} if the source was not an expression
     */
    static PaintExpression of(String source) {
        return new PaintExpression(source);
    }

    /**
     * Works the value out for the frame being drawn.
     *
     * @param frame what the screen and the object being drawn currently measure
     * @param percentOf what a trailing {@code %} is a percentage of — the screen size along the
     *     axis the property belongs to, so {@code 50%} means half the width for {@code x} and half
     *     the height for {@code y}
     * @return the value in GUI pixels, or {@code 0} if it cannot be worked out on this screen
     */
    int get(Frame frame, int percentOf) {
        var value = root.get(frame, percentOf);

        if (!Double.isFinite(value)) {
            report("'" + source + "' does not work out to a number here");
            return 0;
        }

        return (int) value;
    }

    private static void report(String message) {
        if (REPORTED.size() < REPORT_LIMIT && REPORTED.add(message)) {
            ConsoleJS.CLIENT.warn(message + ", so it is being drawn as 0");
        }
    }

    /**
     * What the variables in an expression stand for while one frame is drawn.
     *
     * @param screenWidth {@code $SW}
     * @param screenHeight {@code $SH}
     * @param selfWidth {@code $W}, or {@link #UNKNOWN} while the width itself is being read
     * @param selfHeight {@code $H}, or {@link #UNKNOWN} while the height itself is being read
     */
    record Frame(int screenWidth, int screenHeight, int selfWidth, int selfHeight) {
    }

    @FunctionalInterface
    private interface Node {

        double get(Frame frame, int percentOf);
    }

    /**
     * Recursive descent over the source, once, into composed {@link Node}s.
     *
     * <p>By hand rather than through a scripting engine because this runs on the render thread:
     * the parse happens once, but the tree it leaves behind is walked every frame, and a walk of
     * boxed lambdas is as much as that can afford.
     */
    private static final class Parser {

        private final String source;

        private int index;

        Parser(String source) {
            this.source = source;
        }

        Node parse() {
            var node = expression();
            skipSpace();

            if (index < source.length()) {
                throw new IllegalArgumentException(
                    "'" + source + "' has a stray '" + source.charAt(index) + "' in it");
            }

            return node;
        }

        private Node expression() {
            var node = term();

            while (true) {
                skipSpace();

                if (eat('+')) {
                    node = combine(node, term(), Double::sum);
                } else if (eat('-')) {
                    node = combine(node, term(), (left, right) -> left - right);
                } else {
                    return node;
                }
            }
        }

        private Node term() {
            var node = factor();

            while (true) {
                skipSpace();

                if (eat('*')) {
                    node = combine(node, factor(), (left, right) -> left * right);
                } else if (eat('/')) {
                    node = combine(node, factor(), (left, right) -> left / right);
                } else {
                    return node;
                }
            }
        }

        private Node factor() {
            skipSpace();

            if (eat('-')) {
                var operand = factor();
                return (frame, percentOf) -> -operand.get(frame, percentOf);
            }

            if (eat('+')) {
                return factor();
            }

            var node = primary();
            skipSpace();

            if (eat('%')) {
                return (frame, percentOf) -> node.get(frame, percentOf) / 100D * percentOf;
            }

            return node;
        }

        private Node primary() {
            skipSpace();

            if (eat('(')) {
                var node = expression();
                skipSpace();

                if (!eat(')')) {
                    throw new IllegalArgumentException("'" + source + "' is missing a ')'");
                }

                return node;
            }

            return eat('$') ? variable() : number();
        }

        private Node variable() {
            var start = index;

            while (index < source.length() && Character.isLetterOrDigit(source.charAt(index))) {
                index++;
            }

            var name = source.substring(start, index);

            return switch (name.toUpperCase(Locale.ROOT)) {
                case "SW" -> (frame, percentOf) -> frame.screenWidth();
                case "SH" -> (frame, percentOf) -> frame.screenHeight();
                case "W" -> (frame, percentOf) -> resolve(frame.selfWidth());
                case "H" -> (frame, percentOf) -> resolve(frame.selfHeight());
                default -> throw new IllegalArgumentException(
                    "'$" + name + "' is not something a description can measure against");
            };
        }

        private Node number() {
            var start = index;

            while (index < source.length()
                && (Character.isDigit(source.charAt(index)) || source.charAt(index) == '.')) {
                index++;
            }

            var text = source.substring(start, index);

            try {
                var value = Double.parseDouble(text);
                return (frame, percentOf) -> value;
            } catch (NumberFormatException failure) {
                throw new IllegalArgumentException(
                    "'" + source + "' is not a number or an expression");
            }
        }

        private void skipSpace() {
            while (index < source.length() && Character.isWhitespace(source.charAt(index))) {
                index++;
            }
        }

        private boolean eat(char character) {
            if (index < source.length() && source.charAt(index) == character) {
                index++;
                return true;
            }

            return false;
        }

        private static Node combine(Node left, Node right, DoubleBinaryOperator operator) {
            return (frame, percentOf) -> operator.applyAsDouble(
                left.get(frame, percentOf), right.get(frame, percentOf));
        }

        /** Not-a-number rather than zero, so an unknown size is reported instead of drawn. */
        private static double resolve(int size) {
            return size == UNKNOWN ? Double.NaN : size;
        }
    }
}

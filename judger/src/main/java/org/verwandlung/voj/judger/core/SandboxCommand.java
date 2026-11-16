/* Verwandlung Online Judge - A cross-platform judge online system
 * Copyright (C) 2014-2026 Haozhe Xie <root@haozhexie.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.verwandlung.voj.judger.core;

import java.util.List;

/**
 * A compilation or run, as a sandbox is to carry it out. Built by {@link SandboxPolicy}, the one
 * place that decides what a program gets; a sandbox only enforces it.
 *
 * @param arguments - the program (an absolute path where it was found) and its arguments
 * @param environment - the whole environment of the program, as {@code NAME=value}
 * @param workDirectory - the directory the program runs in, its home and temporary directory, and
 *     the only one it may write to: the program directory for a compilation, a fresh directory next
 *     to it for a run (see {@link Preprocessor})
 * @param inputFile - the file standard input is read from ({@code null}: none)
 * @param outputFile - the file standard output goes to ({@code null}: none), in a directory the
 *     program may not write to; standard error goes there too for a compilation (the compile log),
 *     never for a run, whose output is judged
 * @param compilation - whether a trusted compiler runs (which may start processes), rather than the
 *     submission
 * @param limits - the limits of the program
 * @author Haozhe Xie
 */
public record SandboxCommand(
    List<String> arguments,
    List<String> environment,
    String workDirectory,
    String inputFile,
    String outputFile,
    boolean compilation,
    Limits limits) {}

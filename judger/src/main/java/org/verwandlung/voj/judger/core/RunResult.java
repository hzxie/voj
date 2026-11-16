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

/**
 * The outcome of a run of a submission on one checkpoint, before its output is compared.
 *
 * @param verdict - how the run ended ({@link Verdict#NORMAL}: its output is up to the comparison)
 * @param usedTime - the CPU time it used (ms), at least the limit if it exceeded it
 * @param usedMemory - the memory it used (KB), at least the limit if it exceeded it
 * @author Haozhe Xie
 */
public record RunResult(Verdict verdict, int usedTime, int usedMemory) {}

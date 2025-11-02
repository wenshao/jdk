/*
 * Copyright (c) 2025, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 *
 */

#ifndef SHARE_OPTO_STRINGINDEXER_HPP
#define SHARE_OPTO_STRINGINDEXER_HPP

#include "opto/node.hpp"
#include "opto/phaseX.hpp"

class StringIndexer : public StackObj {
 private:
  PhaseGVN* _gvn;
  Node* _str;
  Node* _index;
  int _start_idx;
  int _end_idx;

 public:
  StringIndexer(PhaseGVN* gvn, Node* str, Node* index) : 
    _gvn(gvn), _str(str), _index(index), _start_idx(0), _end_idx(0) {}

  // Check if the string indexing operation can be optimized
  bool can_optimize();

  // Perform the optimization
  Node* optimize();

 private:
  // Helper methods for optimization
  bool is_constant_string();
  bool is_valid_range();
  Node* generate_optimized_code();
};

#endif // SHARE_OPTO_STRINGINDEXER_HPP
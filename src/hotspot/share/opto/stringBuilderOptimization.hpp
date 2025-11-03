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
 */

#ifndef SHARE_OPTO_STRINGBUILDEROPTIMIZATION_HPP
#define SHARE_OPTO_STRINGBUILDEROPTIMIZATION_HPP

#include "opto/node.hpp"
#include "opto/phaseX.hpp"

class CallStaticJavaNode;

class StringBuilderOptimization : public StackObj {
private:
  PhaseGVN* _gvn;

public:
  StringBuilderOptimization(PhaseGVN* gvn);
  
  // Main entry point for the optimization
  void optimize();
  
  // Check if a call is StringBuilder.append(char)
  bool is_append_char_call(CallStaticJavaNode* call);
  
  // Check if a call is StringBuilder.append(char, char)
  bool is_append_char_char_call(CallStaticJavaNode* call);
  
  // Optimize consecutive StringBuilder.append(char) calls
  void optimize_append_char_call(CallStaticJavaNode* call, Unique_Node_List& worklist);
  
  // Find the next consecutive StringBuilder.append(char) call
  CallStaticJavaNode* find_next_append_char_call(CallStaticJavaNode* call);
  
  // Create a new call to StringBuilder.append(char, char)
  CallStaticJavaNode* create_append_char_char_call(
      CallStaticJavaNode* original_call, Node* receiver, Node* char1, Node* char2);
};

#endif // SHARE_OPTO_STRINGBUILDEROPTIMIZATION_HPP
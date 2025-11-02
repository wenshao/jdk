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

#include "ci/ciTypeArray.hpp"
#include "classfile/javaClasses.hpp"
#include "opto/idealKit.hpp"
#include "opto/graphKit.hpp"
#include "opto/stringIndexer.hpp"

#define __ kit.

// Check if the string indexing operation can be optimized
bool StringIndexer::can_optimize() {
  // For now, we only optimize constant strings with constant indices
  return is_constant_string() && is_valid_range();
}

// Perform the optimization
Node* StringIndexer::optimize() {
  if (!can_optimize()) {
    return nullptr;
  }

  return generate_optimized_code();
}

// Helper methods for optimization
bool StringIndexer::is_constant_string() {
  return _str->is_Con() && _str->bottom_type()->isa_oopptr() &&
         _str->bottom_type()->isa_oopptr()->const_oop() != nullptr;
}

bool StringIndexer::is_valid_range() {
  if (!_index->is_Con()) {
    return false;
  }

  const TypeInt* index_type = _index->bottom_type()->is_int();
  if (!index_type->is_con()) {
    return false;
  }

  int index = index_type->get_con();
  if (index < 0) {
    return false;
  }

  // Get the string length
  ciInstance* str_instance = _str->bottom_type()->isa_oopptr()->const_oop()->as_instance();
  ciObject* value_array = str_instance->field_value_by_offset(java_lang_String::value_offset()).as_object();
  if (value_array == nullptr || !value_array->is_type_array()) {
    return false;
  }

  ciTypeArray* array = value_array->as_type_array();
  int length = array->length();

  // For UTF16 strings, the length in the array is twice the string length
  jbyte coder = str_instance->field_value_by_offset(java_lang_String::coder_offset()).as_byte();
  if (coder == java_lang_String::CODER_UTF16) {
    length /= 2;
  }

  _start_idx = index;
  _end_idx = index + 1;

  return index < length;
}

Node* StringIndexer::generate_optimized_code() {
  // Get the string value array
  ciInstance* str_instance = _str->bottom_type()->isa_oopptr()->const_oop()->as_instance();
  ciObject* value_array = str_instance->field_value_by_offset(java_lang_String::value_offset()).as_object();
  ciTypeArray* array = value_array->as_type_array();

  // Get the string coder
  jbyte coder = str_instance->field_value_by_offset(java_lang_String::coder_offset()).as_byte();

  // Get the character at the specified index
  jchar ch;
  if (coder == java_lang_String::CODER_LATIN1) {
    ch = (jchar)(array->byte_at(_start_idx) & 0xFF);
  } else {
    // UTF16 - need to read two bytes
    int shift_high, shift_low;
#ifdef VM_LITTLE_ENDIAN
    shift_high = 0;
    shift_low = 8;
#else
    shift_high = 8;
    shift_low = 0;
#endif
    jchar b1 = ((jchar) array->byte_at(_start_idx * 2)) & 0xff;
    jchar b2 = ((jchar) array->byte_at(_start_idx * 2 + 1)) & 0xff;
    ch = (b1 << shift_high) | (b2 << shift_low);
  }

  // Create a constant node for the character
  return _gvn->intcon(ch);
}

#undef __
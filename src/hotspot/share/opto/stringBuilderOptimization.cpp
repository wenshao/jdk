#include "opto/stringBuilderOptimization.hpp"
#include "opto/callnode.hpp"
#include "opto/phaseX.hpp"
#include "opto/compile.hpp"
#include "opto/graphKit.hpp"
#include "opto/idealKit.hpp"
#include "opto/rootnode.hpp"
#include "ci/ciSymbols.hpp"

// Constructor
StringBuilderOptimization::StringBuilderOptimization(PhaseGVN* gvn) : _gvn(gvn) {}

// Main entry point for the optimization
void StringBuilderOptimization::optimize() {
  // This optimization is only enabled when OptimizeStringConcat is true
  if (!OptimizeStringConcat) {
    return;
  }
  
  // Get all CallStaticJavaNode nodes in the graph
  Unique_Node_List worklist;
  
  // Populate worklist with all CallStaticJavaNode nodes
  for (uint i = 0; i < _gvn->C->unique(); i++) {
    Node* n = _gvn->C->root()->find(i);
    if (n != nullptr && n->is_CallStaticJava()) {
      CallStaticJavaNode* call = n->as_CallStaticJava();
      if (is_append_char_call(call)) {
        worklist.push(call);
      }
    }
  }
  
  // Process the worklist
  while (worklist.size() > 0) {
    Node* n = worklist.pop();
    if (n->is_CallStaticJava()) {
      CallStaticJavaNode* call = n->as_CallStaticJava();
      if (is_append_char_call(call)) {
        // Try to optimize this call
        optimize_append_char_call(call, worklist);
      }
    }
  }
}

// Check if a call is StringBuilder.append(char)
bool StringBuilderOptimization::is_append_char_call(CallStaticJavaNode* call) {
  if (call->method() == nullptr) {
    return false;
  }
  
  ciMethod* method = call->method();
  if (method->holder() != _gvn->C->env()->StringBuilder_klass() &&
      method->holder() != _gvn->C->env()->StringBuffer_klass()) {
    return false;
  }
  
  if (method->name() != ciSymbols::append_name()) {
    return false;
  }
  
  // Check signature - should be append(char)
  ciSymbol* char_sig = ciSymbols::char_signature();
  if (method->holder() == _gvn->C->env()->StringBuffer_klass()) {
    char_sig = ciSymbols::char_signature();
  }
  
  return method->signature()->as_symbol() == char_sig;
}

// Check if a call is StringBuilder.append(char, char)
bool StringBuilderOptimization::is_append_char_char_call(CallStaticJavaNode* call) {
  if (call->method() == nullptr) {
    return false;
  }
  
  ciMethod* method = call->method();
  if (method->holder() != _gvn->C->env()->StringBuilder_klass() &&
      method->holder() != _gvn->C->env()->StringBuffer_klass()) {
    return false;
  }
  
  if (method->name() != ciSymbols::append_name()) {
    return false;
  }
  
  // Check signature - should be append(char, char)
  ciSymbol* char_char_sig = ciSymbols::char_char_signature();
  if (method->holder() == _gvn->C->env()->StringBuffer_klass()) {
    // There is no char_char signature for StringBuffer, so we'll use the StringBuilder one
    char_char_sig = ciSymbols::char_char_signature();
  }
  
  return method->signature()->as_symbol() == char_char_sig;
}

// Optimize consecutive StringBuilder.append(char) calls
void StringBuilderOptimization::optimize_append_char_call(CallStaticJavaNode* call, Unique_Node_List& worklist) {
  // Get the receiver object (StringBuilder instance)
  Node* receiver = call->in(TypeFunc::Parms);
  
  // Get the first char argument
  Node* char1 = call->in(TypeFunc::Parms + 1);
  
  // Look for the next call in the chain
  CallStaticJavaNode* next_call = find_next_append_char_call(call);
  
  if (next_call != nullptr) {
    // Get the second char argument
    Node* char2 = next_call->in(TypeFunc::Parms + 1);
    
    // Create a new call to append(char, char)
    CallStaticJavaNode* new_call = create_append_char_char_call(call, receiver, char1, char2);
    
    if (new_call != nullptr) {
      // Replace the first call with the new call
      _gvn->C->gvn_replace_by(call, new_call);
      
      // Remove the second call from the graph
      _gvn->C->gvn_replace_by(next_call, next_call->in(TypeFunc::Parms));
      
      // Add the new call to the worklist for further optimization
      worklist.push(new_call);
    }
  }
}

// Find the next consecutive StringBuilder.append(char) call
CallStaticJavaNode* StringBuilderOptimization::find_next_append_char_call(CallStaticJavaNode* call) {
  // Get the control output of the call
  Node* ctrl = call->proj_out_or_null(TypeFunc::Control);
  if (ctrl == nullptr) {
    return nullptr;
  }
  
  // Follow the control flow to find the next call
  for (DUIterator_Fast imax, i = ctrl->fast_outs(imax); i < imax; i++) {
    Node* use = ctrl->fast_out(i);
    if (use->is_Proj() && use->as_Proj()->_con == TypeFunc::Control) {
      Node* next_ctrl = use->unique_ctrl_out_or_null();
      if (next_ctrl != nullptr && next_ctrl->is_CallStaticJava()) {
        CallStaticJavaNode* next_call = next_ctrl->as_CallStaticJava();
        // Check if it's an append(char) call on the same receiver
        if (is_append_char_call(next_call) && 
            next_call->in(TypeFunc::Parms) == call->in(TypeFunc::Parms)) {
          return next_call;
        }
      }
    }
  }
  
  return nullptr;
}

// Create a new call to StringBuilder.append(char, char)
CallStaticJavaNode* StringBuilderOptimization::create_append_char_char_call(
    CallStaticJavaNode* original_call, Node* receiver, Node* char1, Node* char2) {
  
  // Get the method signature for append(char, char)
  ciInstanceKlass* sb_klass = _gvn->C->env()->StringBuilder_klass();
  ciSymbol* name = ciSymbols::append_name();
  ciSymbol* sig = ciSymbols::char_char_signature();
  
  // Find the method in StringBuilder (it's inherited from AbstractStringBuilder)
  ciMethod* method = sb_klass->find_method(name, sig);
  if (method == nullptr) {
    // If the method doesn't exist, try StringBuffer
    sb_klass = _gvn->C->env()->StringBuffer_klass();
    method = sb_klass->find_method(name, sig);
    if (method == nullptr) {
      return nullptr;
    }
  }
  
  // Create the new call node
  const Type** fields = TypeTuple::fields(TypeFunc::Parms + 3);
  fields[TypeFunc::Parms + 0] = Type::BOTTOM;
  fields[TypeFunc::Parms + 1] = Type::BOTTOM;
  fields[TypeFunc::Parms + 2] = Type::BOTTOM;
  const TypeTuple* domain = TypeTuple::make(TypeFunc::Parms + 3, fields);
  
  fields = TypeTuple::fields(TypeFunc::Parms + 1);
  fields[TypeFunc::Parms + 0] = Type::BOTTOM;
  const TypeTuple* range = TypeTuple::make(TypeFunc::Parms + 1, fields);
  
  const TypeFunc* tf = TypeFunc::make(domain, range);
  
  CallStaticJavaNode* new_call = new CallStaticJavaNode(_gvn->C, tf, 0, method);
  new_call->init_req(0, original_call->in(0)); // Control
  new_call->init_req(1, original_call->in(1)); // I/O
  new_call->init_req(2, original_call->in(2)); // Memory
  new_call->init_req(3, original_call->in(3)); // FramePtr
  new_call->init_req(4, original_call->in(4)); // ReturnAdr
  new_call->init_req(5, receiver);             // Receiver
  new_call->init_req(6, char1);                // First char
  new_call->init_req(7, char2);                // Second char
  
  // Transform the new node
  _gvn->transform(new_call);
  
  return new_call;
}
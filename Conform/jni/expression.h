#include "exprtk.hpp"

template<typename T>
class Expression {
   typedef exprtk::symbol_table<T> symbol_table_t;
   typedef exprtk::expression<T>     expression_t;
   typedef exprtk::parser<T>             parser_t;

   const std::string m_expression_string;
   expression_t m_expression;
   T m_z;

public:
   Expression(const std::string &expression_string) : m_expression_string(expression_string) {
		symbol_table_t symbol_table;
		symbol_table.add_variable("z",m_z);
		symbol_table.add_constants();
		m_expression.register_symbol_table(symbol_table);
		parser_t parser;
		parser.compile(m_expression_string,m_expression);
   }

   T& getIndepVar() {
	   return m_z;
   }

   T value() const {
	   return m_expression.value();
   }

   T valueFor(const T& zin) {
	   m_z = zin;
	   return m_expression.value();
   }

};



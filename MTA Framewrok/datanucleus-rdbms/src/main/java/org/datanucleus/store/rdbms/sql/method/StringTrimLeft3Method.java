/**********************************************************************
Copyright (c) 2010 Andy Jefferson and others. All rights reserved.
Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

Contributors:
   ...
**********************************************************************/
package org.datanucleus.store.rdbms.sql.method;

/**
 * Method for evaluating {strExpr1}.trimLeft() or "TRIM(LEADING trimChar FROM strExpr1)".
 * Returns a StrignExpression that equates to <pre>TRIM([[LEADING] [<trim_char>] FROM] strExpr)</pre>
 */
public class StringTrimLeft3Method extends StringTrim3Method
{
    protected String getTrimSpecKeyword()
    {
        return "LEADING";
    }
}

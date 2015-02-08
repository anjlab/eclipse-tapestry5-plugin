// Copyright 2006, 2007 The Apache Software Foundation
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.anjlab.eclipse.tapestry5.internal;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.anjlab.eclipse.tapestry5.TapestrySymbol;

/**
 * Contains execution data needed when performing an expansion (largely, to check for endless recursion).
 */
public class SymbolExpansion
{
    private final Map<String, String> cache = new HashMap<String, String>();
    
    private final LinkedList<String> expandingSymbols = new LinkedList<String>();
    
    private final Map<String, List<TapestrySymbol>> symbols;
    
    public SymbolExpansion(Map<String, List<TapestrySymbol>> symbols)
    {
        this.symbols = symbols;
    }
    
    public String expandSymbols(String input)
    {
        StringBuilder builder = null;

        int startx = 0;

        while (true)
        {
            int symbolx = input.indexOf("${", startx);

            // Special case: if the string contains no symbols then return it as is.

            if (startx == 0 && symbolx < 0) return input;

            // The string has at least one symbol, so its OK to create the StringBuilder

            if (builder == null) builder = new StringBuilder();

            // No more symbols found, so add in the rest of the string.

            if (symbolx < 0)
            {
                builder.append(input.substring(startx));
                break;
            }

            builder.append(input.substring(startx, symbolx));

            int endx = input.indexOf("}", symbolx);

            if (endx < 0)
            {
                String message = expandingSymbols.isEmpty()
                        ? String.format("Input string '%s' is missing a symbol closing brace.", input)
                        : String.format("Input string '%s' is missing a symbol closing brace (in %s).", input, path());

                throw new RuntimeException(message);
            }

            String symbolName = input.substring(symbolx + 2, endx);

            builder.append(valueForSymbol(symbolName));

            // Restart the search after the '}'

            startx = endx + 1;
        }

        return builder.toString();
    }

    String valueForSymbol(String symbolName)
    {
        String value = cache.get(symbolName);

        if (value == null)
        {
            value = expandSymbol(symbolName);

            cache.put(symbolName, value);
        }

        return value;
    }

    String expandSymbol(String symbolName)
    {
        if (expandingSymbols.contains(symbolName))
        {
            expandingSymbols.add(symbolName);
            throw new RuntimeException(String.format("Symbol '%s' is defined in terms of itself (%s).",
                    symbolName,
                    pathFrom(symbolName)));
        }

        expandingSymbols.addLast(symbolName);

        String value = null;

        List<TapestrySymbol> values = symbols.get(symbolName);
        
        if (values != null)
        {
            for (TapestrySymbol symbol : values)
            {
                if (!symbol.isOverridden())
                {
                    value = symbol.getValue();
                    //  XXX Throw exception if value matches "<.*>" (i.e. evaluation failed)
                    break;
                }
            }
        }

        if (value == null)
        {

            String message = expandingSymbols.size() == 1
                    ? String.format("Symbol '%s' is not defined.", symbolName)
                    : String.format("Symbol '%s' is not defined (in %s).", symbolName, path());

            throw new RuntimeException(message);
        }

        // The value may have symbols that need expansion.

        String result = expandSymbols(value);

        // And we're done expanding this symbol

        expandingSymbols.removeLast();

        return result;

    }

    String path()
    {
        StringBuilder builder = new StringBuilder();

        boolean first = true;

        for (String symbolName : expandingSymbols)
        {
            if (!first) builder.append(" --> ");

            builder.append(symbolName);

            first = false;
        }

        return builder.toString();
    }

    String pathFrom(String startSymbolName)
    {
        StringBuilder builder = new StringBuilder();

        boolean first = true;
        boolean match = false;

        for (String symbolName : expandingSymbols)
        {
            if (!match)
            {
                if (symbolName.equals(startSymbolName))
                    match = true;
                else
                    continue;
            }

            if (!first) builder.append(" --> ");

            builder.append(symbolName);

            first = false;
        }

        return builder.toString();
    }
}

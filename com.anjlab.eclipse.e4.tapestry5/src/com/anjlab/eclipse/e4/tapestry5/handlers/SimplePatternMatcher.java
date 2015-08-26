package com.anjlab.eclipse.e4.tapestry5.handlers;

import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

public class SimplePatternMatcher implements PatternMatcher
{

    private final Pattern pattern;

    public SimplePatternMatcher(String pattern)
    {
        pattern = StringUtils.trimToEmpty(pattern);
        StringBuilder builder = new StringBuilder(".*");
        String[] segments = pattern.split("\\*");
        for (String segment : segments)
        {
            builder.append(Pattern.quote(segment)).append(".*");
        }
        this.pattern = Pattern.compile(
                builder.toString(),
                Pattern.CASE_INSENSITIVE);
    }

    @Override
    public boolean matches(String input)
    {
        input = StringUtils.trimToNull(input);

        if (input == null)
        {
            return false;
        }

        return pattern.matcher(input).find();
    }

}

package com.github.microprograms.micro_api_sdk.utils;

public class JsonPrettyPrinter {
    /**
     * Amount of spaces inserted at each indentation level.
     */
    final public static int tabWidth = 4;

    /**
     * Repeats a `String` a number of `times`.
     * 
     * @param str
     *            The string to be repeated.
     * @param times
     *            Number of times `str` should be repeated.
     * @return Returns the repeated string.
     */
    private static String repeatString(String str, int times) {
        assert (times >= 0);

        String result = "";
        for (int i = 0; i != times; ++i) {
            result += str;
        }
        return result;
    }

    /**
     * Formats a JSON-String into a "pretty-printed" more human readable format.
     * 
     * @param json
     *            The JSON-String to be formatted. This needs to be a minimal
     *            JSON, meaning no whitespace or newlines inside it.
     * @return The formatted JSON-String.
     */
    public static String format(String json) {
        int indent = 0;
        boolean inString = false;
        boolean inEscape = false;

        String result = "";

        // We could change this method of iterating over string by using
        // reflection, this would improve performance for strings longer than
        // 500 chars. See:
        // http://stackoverflow.com/questions/8894258/fastest-way-to-iterate-over-all-the-chars-in-a-string
        for (char c : json.toCharArray()) {
            switch (c) {
            case '}':
            case ']':
                if (!inString) {
                    --indent;
                    result += "\n" + repeatString(" ", indent * tabWidth);
                }
                break;
            }

            result += c;

            switch (c) {
            case '{':
            case '[':
                if (!inString) {
                    ++indent;
                    result += "\n" + repeatString(" ", indent * tabWidth);
                }
                break;

            case ':':
                if (!inString)
                    result += " ";
                break;

            case ',':
                if (!inString)
                    result += "\n" + repeatString(" ", indent * tabWidth);
                break;

            case '"':
                if (!inEscape)
                    inString = !inString;
                break;
            }

            if (inEscape)
                inEscape = false;
            else if (c == '\\')
                inEscape = true;
        }

        return result;
    }
}

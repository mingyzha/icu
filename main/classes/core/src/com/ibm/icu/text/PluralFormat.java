/*
 *******************************************************************************
 * Copyright (C) 2007-2011, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */

package com.ibm.icu.text;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.text.FieldPosition;
import java.text.ParsePosition;
import java.util.Map;

import com.ibm.icu.impl.Utility;
import com.ibm.icu.util.ULocale;

/**
 * <p>
 * <code>PluralFormat</code> supports the creation of internationalized
 * messages with plural inflection. It is based on <i>plural
 * selection</i>, i.e. the caller specifies messages for each
 * plural case that can appear in the user's language and the
 * <code>PluralFormat</code> selects the appropriate message based on
 * the number.
 * </p>
 * <h4>The Problem of Plural Forms in Internationalized Messages</h4>
 * <p>
 * Different languages have different ways to inflect
 * plurals. Creating internationalized messages that include plural
 * forms is only feasible when the framework is able to handle plural
 * forms of <i>all</i> languages correctly. <code>ChoiceFormat</code>
 * doesn't handle this well, because it attaches a number interval to
 * each message and selects the message whose interval contains a
 * given number. This can only handle a finite number of
 * intervals. But in some languages, like Polish, one plural case
 * applies to infinitely many intervals (e.g., the paucal case applies to
 * numbers ending with 2, 3, or 4 except those ending with 12, 13, or
 * 14). Thus <code>ChoiceFormat</code> is not adequate.
 * </p><p>
 * <code>PluralFormat</code> deals with this by breaking the problem
 * into two parts:
 * <ul>
 * <li>It uses <code>PluralRules</code> that can define more complex
 *     conditions for a plural case than just a single interval. These plural
 *     rules define both what plural cases exist in a language, and to
 *     which numbers these cases apply.
 * <li>It provides predefined plural rules for many languages. Thus, the programmer
 *     need not worry about the plural cases of a language and
 *     does not have to define the plural cases; they can simply
 *     use the predefined keywords. The whole plural formatting of messages can
 *     be done using localized patterns from resource bundles. For predefined plural 
 *     rules, see the CLDR <i>Language Plural Rules</i> page at
 *    http://unicode.org/repos/cldr-tmp/trunk/diff/supplemental/language_plural_rules.html 
 * </ul>
 * </p>
 * <h4>Usage of <code>PluralFormat</code></h4>
 * <p>Note: Typically, plural formatting is done via <code>MessageFormat</code>
 * with a <code>plural</code> argument type,
 * rather than using a stand-alone <code>PluralFormat</code>.
 * </p><p>
 * This discussion assumes that you use <code>PluralFormat</code> with
 * a predefined set of plural rules. You can create one using one of
 * the constructors that takes a <code>ULocale</code> object. To
 * specify the message pattern, you can either pass it to the
 * constructor or set it explicitly using the
 * <code>applyPattern()</code> method. The <code>format()</code>
 * method takes a number object and selects the message of the
 * matching plural case. This message will be returned.
 * </p>
 * <h5>Patterns and Their Interpretation</h5>
 * <p>
 * The pattern text defines the message output for each plural case of the
 * specified locale. Syntax:
 * <blockquote><pre>
 * pluralStyle = [offsetValue] (selector '{' message '}')+
 * offsetValue = "offset:" number
 * selector = explicitValue | keyword
 * explicitValue = '=' number  // adjacent, no white space in between
 * keyword = [^[[:Pattern_Syntax:][:Pattern_White_Space:]]]+
 * message: see {@link MessageFormat}
 * </pre></blockquote>
 * Pattern_White_Space between syntax elements is ignored, except
 * between the {curly braces} and their sub-message,
 * and between the '=' and the number of an explicitValue.
 *
 * </p><p>
 * There are 6 predefined case keywords in CLDR/ICU - 'zero', 'one', 'two', 'few', 'many' and 
 * 'other'. You always have to define a message text for the default plural case 
 * "<code>other</code>" which is contained in every rule set.
 * If you do not specify a message text for a particular plural case, the
 * message text of the plural case "<code>other</code>" gets assigned to this
 * plural case.
 * </p><p>
 * When formatting, the input number is first matched against the explicitValue clauses.
 * If there is no exact-number match, then a keyword is selected by calling
 * the <code>PluralRules</code> with the input number <em>minus the offset</em>.
 * (The offset defaults to 0 if it is omitted from the pattern string.)
 * If there is no clause with that keyword, then the "other" clauses is returned.
 * </p><p>
 * An unquoted pound sign (<code>#</code>) in the selected sub-message
 * itself (i.e., outside of arguments nested in the sub-message)
 * is replaced by the input number minus the offset.
 * The number-minus-offset value is formatted using a
 * <code>NumberFormat</code> for the <code>PluralFormat</code>'s locale. If you
 * need special number formatting, you have to use a <code>MessageFormat</code>
 * and explicitly specify a <code>NumberFormat</code> argument.
 * <strong>Note:</strong> That argument is formatting without subtracting the offset!
 * If you need a custom format and have a non-zero offset, then you need to pass the
 * number-minus-offset value as a separate parameter.
 * </p>
 * For a usage example, see the {@link MessageFormat} class documentation.
 *
 * <h4>Defining Custom Plural Rules</h4>
 * <p>If you need to use <code>PluralFormat</code> with custom rules, you can
 * create a <code>PluralRules</code> object and pass it to
 * <code>PluralFormat</code>'s constructor. If you also specify a locale in this
 * constructor, this locale will be used to format the number in the message
 * texts.
 * </p><p>
 * For more information about <code>PluralRules</code>, see
 * {@link PluralRules}.
 * </p>
 *
 * @author tschumann (Tim Schumann)
 * @stable ICU 3.8
 */
public class PluralFormat extends UFormat {
    private static final long serialVersionUID = 1L;

    /**
     * The locale used for standard number formatting and getting the predefined
     * plural rules (if they were not defined explicitely).
     * @serial
     */
    private ULocale ulocale = null;

    /**
     * The plural rules used for plural selection.
     * @serial
     */
    private PluralRules pluralRules = null;

    /**
     * The applied pattern string.
     * @serial
     */
    private String pattern = null;

    /**
     * The MessagePattern which contains the parsed structure of the pattern string.
     */
    transient private MessagePattern msgPattern;
    
    /**
     * Obsolete with use of MessagePattern since ICU 4.8. Used to be:
     * The format messages for each plural case. It is a mapping:
     *  <code>String</code>(plural case keyword) --&gt; <code>String</code>
     *  (message for this plural case).
     * @serial
     */
    private Map<String, String> parsedValues = null;

    /**
     * This <code>NumberFormat</code> is used for the standard formatting of
     * the number inserted into the message.
     * @serial
     */
    private NumberFormat numberFormat = null;

    /**
     * The offset to subtract before invoking plural rules.
     */
    transient private double offset = 0;

    /**
     * Interface for classes like PluralRules which select PluralFormat keywords for numbers.
     * @internal
     */
    /*package*/ interface PluralSelector {
        /**
         * Given a number, returns the appropriate PluralFormat keyword.
         *
         * @param number The number to be plural-formatted.
         * @return The selected PluralFormat keyword.
         */
        public String select(double number);
    }

    /**
     * Creates a new <code>PluralFormat</code> for the default locale.
     * This locale will be used to get the set of plural rules and for standard
     * number formatting.
     * @stable ICU 3.8
     */
    public PluralFormat() {
        init(null, ULocale.getDefault());
    }

    /**
     * Creates a new <code>PluralFormat</code> for a given locale.
     * @param ulocale the <code>PluralFormat</code> will be configured with
     *        rules for this locale. This locale will also be used for standard
     *        number formatting.
     * @stable ICU 3.8
     */
    public PluralFormat(ULocale ulocale) {
        init(null, ulocale);
    }

    /**
     * Creates a new <code>PluralFormat</code> for a given set of rules.
     * The standard number formatting will be done using the default locale.
     * @param rules defines the behavior of the <code>PluralFormat</code>
     *        object.
     * @stable ICU 3.8
     */
    public PluralFormat(PluralRules rules) {
        init(rules, ULocale.getDefault());
    }

    /**
     * Creates a new <code>PluralFormat</code> for a given set of rules.
     * The standard number formatting will be done using the given locale.
     * @param ulocale the default number formatting will be done using this
     *        locale.
     * @param rules defines the behavior of the <code>PluralFormat</code>
     *        object.
     * @stable ICU 3.8
     */
    public PluralFormat(ULocale ulocale, PluralRules rules) {
        init(rules, ulocale);
    }

    /**
     * Creates a new <code>PluralFormat</code> for a given pattern string.
     * The default locale will be used to get the set of plural rules and for
     * standard number formatting.
     * @param  pattern the pattern for this <code>PluralFormat</code>.
     * @throws IllegalArgumentException if the pattern is invalid.
     * @stable ICU 3.8
     */
    public PluralFormat(String pattern) {
        init(null, ULocale.getDefault());
        applyPattern(pattern);
    }

    /**
     * Creates a new <code>PluralFormat</code> for a given pattern string and
     * locale.
     * The locale will be used to get the set of plural rules and for
     * standard number formatting.
     * @param ulocale the <code>PluralFormat</code> will be configured with
     *        rules for this locale. This locale will also be used for standard
     *        number formatting.
     * @param  pattern the pattern for this <code>PluralFormat</code>.
     * @throws IllegalArgumentException if the pattern is invalid.
     * @stable ICU 3.8
     */
    public PluralFormat(ULocale ulocale, String pattern) {
        init(null, ulocale);
        applyPattern(pattern);
    }

    /**
     * Creates a new <code>PluralFormat</code> for a given set of rules and a
     * pattern.
     * The standard number formatting will be done using the default locale.
     * @param rules defines the behavior of the <code>PluralFormat</code>
     *        object.
     * @param  pattern the pattern for this <code>PluralFormat</code>.
     * @throws IllegalArgumentException if the pattern is invalid.
     * @stable ICU 3.8
     */
    public PluralFormat(PluralRules rules, String pattern) {
        init(rules, ULocale.getDefault());
        applyPattern(pattern);
    }

    /**
     * Creates a new <code>PluralFormat</code> for a given set of rules, a
     * pattern and a locale.
     * @param ulocale the <code>PluralFormat</code> will be configured with
     *        rules for this locale. This locale will also be used for standard
     *        number formatting.
     * @param rules defines the behavior of the <code>PluralFormat</code>
     *        object.
     * @param  pattern the pattern for this <code>PluralFormat</code>.
     * @throws IllegalArgumentException if the pattern is invalid.
     * @stable ICU 3.8
     */
    public PluralFormat(ULocale ulocale, PluralRules rules, String pattern) {
        init(rules, ulocale);
        applyPattern(pattern);
    }

    /*
     * Initializes the <code>PluralRules</code> object.
     * Postcondition:<br/>
     *   <code>ulocale</code>    :  is <code>locale</code><br/>
     *   <code>pluralRules</code>:  if <code>rules</code> != <code>null</code>
     *                              it's set to rules, otherwise it is the
     *                              predefined plural rule set for the locale
     *                              <code>ulocale</code>.<br/>
     *   <code>parsedValues</code>: is <code>null</code><br/>
     *   <code>pattern</code>:      is <code>null</code><br/>
     *   <code>numberFormat</code>: a <code>NumberFormat</code> for the locale
     *                              <code>ulocale</code>.
     */
    private void init(PluralRules rules, ULocale locale) {
        ulocale = locale;
        pluralRules = (rules == null) ? PluralRules.forLocale(ulocale)
                                      : rules;
        resetPattern();
        numberFormat = NumberFormat.getInstance(ulocale);
        offset = 0;
    }

    private void resetPattern() {
        pattern = null;
        if(msgPattern != null) {
            msgPattern.clear();
        }
    }

    /**
     * Sets the pattern used by this plural format.
     * The method parses the pattern and creates a map of format strings
     * for the plural rules.
     * Patterns and their interpretation are specified in the class description.
     *
     * @param pattern the pattern for this plural format.
     * @throws IllegalArgumentException if the pattern is invalid.
     * @stable ICU 3.8
     */
    public void applyPattern(String pattern) {
        this.pattern = pattern;
        if (msgPattern == null) {
            msgPattern = new MessagePattern();
        }
        try {
            msgPattern.parsePluralStyle(pattern);
            offset = msgPattern.getPluralOffset(0);
        } catch(RuntimeException e) {
            resetPattern();
            throw e;
        }
    }

    /**
     * Returns the pattern for this PluralFormat.
     *
     * @return the pattern string
     * @stable ICU 4.2
     */
    public String toPattern() {
        return pattern;
    }

    /**
     * Finds the PluralFormat sub-message for the given number, or the "other" sub-message.
     * @param pattern A MessagePattern.
     * @param partIndex the index of the first PluralFormat argument style part.
     * @param selector the PluralSelector for mapping the number (minus offset) to a keyword.
     * @param number a number to be matched to one of the PluralFormat argument's explicit values,
     *        or mapped via the PluralSelector.
     * @return the sub-message start part index.
     */
    /*package*/ static int findSubMessage(
            MessagePattern pattern, int partIndex,
            PluralSelector selector, double number) {
        int count=pattern.countParts();
        double offset;
        MessagePattern.Part part=pattern.getPart(partIndex);
        if(part.getType().hasNumericValue()) {
            offset=pattern.getNumericValue(part);
            ++partIndex;
        } else {
            offset=0;
        }
        // The keyword is null until we need to match against non-explicit, not-"other" value.
        // Then we get the keyword from the selector.
        // (In other words, we never call the selector if we match against an explicit value,
        // or if the only non-explicit keyword is "other".)
        String keyword=null;
        // When we find a match, we set msgStart>0 and also set this boolean to true
        // to avoid matching the keyword again (duplicates are allowed)
        // while we continue to look for an explicit-value match.
        boolean haveKeywordMatch=false;
        // msgStart is 0 until we find any appropriate sub-message.
        // We remember the first "other" sub-message if we have not seen any
        // appropriate sub-message before.
        // We remember the first matching-keyword sub-message if we have not seen
        // one of those before.
        // (The parser allows [does not check for] duplicate keywords.
        // We just have to make sure to take the first one.)
        // We avoid matching the keyword twice by also setting haveKeywordMatch=true
        // at the first keyword match.
        // We keep going until we find an explicit-value match or reach the end of the plural style.
        int msgStart=0;
        // Iterate over (ARG_SELECTOR [ARG_INT|ARG_DOUBLE] message) tuples
        // until ARG_LIMIT or end of plural-only pattern.
        do {
            part=pattern.getPart(partIndex++);
            MessagePattern.Part.Type type=part.getType();
            if(type==MessagePattern.Part.Type.ARG_LIMIT) {
                break;
            }
            assert type==MessagePattern.Part.Type.ARG_SELECTOR;
            // part is an ARG_SELECTOR followed by an optional explicit value, and then a message
            if(pattern.getPartType(partIndex).hasNumericValue()) {
                // explicit value like "=2"
                part=pattern.getPart(partIndex++);
                if(number==pattern.getNumericValue(part)) {
                    // matches explicit value
                    return partIndex;
                }
            } else if(!haveKeywordMatch) {
                // plural keyword like "few" or "other"
                // Compare "other" first and call the selector if this is not "other".
                if(pattern.partSubstringMatches(part, "other")) {
                    if(msgStart==0) {
                        msgStart=partIndex;
                        if(keyword!=null && keyword.equals("other")) {
                            // This is the first "other" sub-message,
                            // and the selected keyword is also "other".
                            // Do not match "other" again.
                            haveKeywordMatch=true;
                        }
                    }
                } else {
                    if(keyword==null) {
                        keyword=selector.select(number-offset);
                        if(msgStart!=0 && keyword.equals("other")) {
                            // We have already seen an "other" sub-message.
                            // Do not match "other" again.
                            haveKeywordMatch=true;
                            continue;
                        }
                    }
                    if(pattern.partSubstringMatches(part, keyword)) {
                        // keyword matches
                        msgStart=partIndex;
                        // Do not match this keyword again.
                        haveKeywordMatch=true;
                    }
                }
            }
            partIndex=pattern.getLimitPartIndex(partIndex);
        } while(++partIndex<count);
        return msgStart;
    }

    /**
     * Formats a plural message for a given number.
     *
     * @param number a number for which the plural message should be formatted.
     *        If no pattern has been applied to this
     *        <code>PluralFormat</code> object yet, the formatted number will
     *        be returned.
     * @return the string containing the formatted plural message.
     * @stable ICU 4.0
     */
    public final String format(double number) {
        // If no pattern was applied, return the formatted number.
        if (msgPattern == null || msgPattern.countParts() == 0) {
            return numberFormat.format(number);
        }

        // Get the appropriate sub-message.
        int partIndex = findSubMessage(msgPattern, 0, pluralRules, number);
        // Replace syntactic # signs in the top level of this sub-message
        // (not in nested arguments) with the formatted number-offset.
        number -= offset;
        StringBuilder result = null;
        int prevIndex = msgPattern.getPart(partIndex).getLimit();
        for (;;) {
            MessagePattern.Part part = msgPattern.getPart(++partIndex);
            MessagePattern.Part.Type type = part.getType();
            if (type == MessagePattern.Part.Type.MSG_LIMIT) {
                int index = part.getIndex();
                if (result == null) {
                    return pattern.substring(prevIndex, index);
                } else {
                    return result.append(pattern, prevIndex, index).toString();
                }
            } else if (type == MessagePattern.Part.Type.REPLACE_NUMBER) {
                if (result == null) {
                    result = new StringBuilder();
                }
                int index = part.getIndex();
                result.append(pattern, prevIndex, index);
                result.append(numberFormat.format(number));
                prevIndex = part.getLimit();
            } else if (type == MessagePattern.Part.Type.ARG_START) {
                // Skip arguments so that we do not look at MSG_LIMIT or REPLACE_NUMBER inside them.
                partIndex = msgPattern.getLimitPartIndex(partIndex);
            }
        }
    }

    /**
     * Formats a plural message for a given number and appends the formatted
     * message to the given <code>StringBuffer</code>.
     * @param number a number object (instance of <code>Number</code> for which
     *        the plural message should be formatted. If no pattern has been
     *        applied to this <code>PluralFormat</code> object yet, the
     *        formatted number will be returned.
     *        Note: If this object is not an instance of <code>Number</code>,
     *              the <code>toAppendTo</code> will not be modified.
     * @param toAppendTo the formatted message will be appended to this
     *        <code>StringBuffer</code>.
     * @param pos will be ignored by this method.
     * @return the string buffer passed in as toAppendTo, with formatted text
     *         appended.
     * @throws IllegalArgumentException if number is not an instance of Number
     * @stable ICU 3.8
     */
    public StringBuffer format(Object number, StringBuffer toAppendTo,
            FieldPosition pos) {
        if (number instanceof Number) {
            toAppendTo.append(format(((Number) number).doubleValue()));
            return toAppendTo;
        }
        throw new IllegalArgumentException("'" + number + "' is not a Number");
    }

    /**
     * This method is not yet supported by <code>PluralFormat</code>.
     * @param text the string to be parsed.
     * @param parsePosition defines the position where parsing is to begin,
     * and upon return, the position where parsing left off.  If the position
     * has not changed upon return, then parsing failed.
     * @return nothing because this method is not yet implemented.
     * @throws UnsupportedOperationException will always be thrown by this method.
     * @stable ICU 3.8
     */
    public Number parse(String text, ParsePosition parsePosition) {
        throw new UnsupportedOperationException();
    }

    /**
     * This method is not yet supported by <code>PluralFormat</code>.
     * @param source the string to be parsed.
     * @param pos defines the position where parsing is to begin,
     * and upon return, the position where parsing left off.  If the position
     * has not changed upon return, then parsing failed.
     * @return nothing because this method is not yet implemented.
     * @throws UnsupportedOperationException will always be thrown by this method.
     * @stable ICU 3.8
     */
    public Object parseObject(String source, ParsePosition pos) {
        throw new UnsupportedOperationException();
    }

    /**
     * Sets the locale used by this <code>PluraFormat</code> object.
     * Note: Calling this method resets this <code>PluraFormat</code> object,
     *     i.e., a pattern that was applied previously will be removed,
     *     and the NumberFormat is set to the default number format for
     *     the locale.  The resulting format behaves the same as one
     *     constructed from {@link #PluralFormat(ULocale)}.
     * @param ulocale the <code>ULocale</code> used to configure the
     *     formatter. If <code>ulocale</code> is <code>null</code>, the
     *     default locale will be used.
     * @stable ICU 3.8
     */
    public void setLocale(ULocale ulocale) {
        if (ulocale == null) {
            ulocale = ULocale.getDefault();
        }
        init(null, ulocale);
    }

    /**
     * Sets the number format used by this formatter.  You only need to
     * call this if you want a different number format than the default
     * formatter for the locale.
     * @param format the number format to use.
     * @stable ICU 3.8
     */
    public void setNumberFormat(NumberFormat format) {
        numberFormat = format;
    }

    /**
     * {@inheritDoc}
     * @stable ICU 3.8
     */
    @Override
    public boolean equals(Object rhs) {
        if(this == rhs) {
            return true;
        }
        if(rhs == null || getClass() != rhs.getClass()) {
            return false;
        }
        PluralFormat pf = (PluralFormat)rhs;
        return
            Utility.objectEquals(ulocale, pf.ulocale) &&
            Utility.objectEquals(pluralRules, pf.pluralRules) &&
            Utility.objectEquals(msgPattern, pf.msgPattern) &&
            Utility.objectEquals(numberFormat, pf.numberFormat);
    }

    /**
     * Returns true if this equals the provided PluralFormat.
     * @param rhs the PluralFormat to compare against
     * @return true if this equals rhs
     * @stable ICU 3.8
     */
    public boolean equals(PluralFormat rhs) {
        return equals((Object)rhs);
    }

    /**
     * {@inheritDoc}
     * @stable ICU 3.8
     */
    @Override
    public int hashCode() {
        return pluralRules.hashCode() ^ parsedValues.hashCode();
    }

    /**
     * {@inheritDoc}
     * @stable ICU 3.8
     */
    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder();
        buf.append("locale=" + ulocale);
        buf.append(", rules='" + pluralRules + "'");
        buf.append(", pattern='" + pattern + "'");
        buf.append(", format='" + numberFormat + "'");
        return buf.toString();
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        // Ignore the parsedValues from an earlier class version (before ICU 4.8)
        // and rebuild the msgPattern.
        parsedValues = null;
        if (pattern != null) {
            applyPattern(pattern);
        }
    }
}

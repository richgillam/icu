// © 2016 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
/*
 *******************************************************************************
 * Copyright (C) 1996-2011, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.text;

import com.ibm.icu.impl.UCaseProps;
import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.util.ULocale;

/**
 * A transliterator that performs locale-sensitive toUpper()
 * case mapping.
 */
class UppercaseTransliterator extends Transliterator {

    /**
     * Package accessible ID.
     */
    static final String _ID = "Any-Upper";
    // TODO: Add variants for tr/az, el, lt, default = default locale: ICU ticket #12720

    /**
     * System registration hook.
     */
    static void register() {
        Transliterator.registerFactory(_ID, new Transliterator.Factory() {
            @Override
            public Transliterator getInstance(String ID) {
                return new UppercaseTransliterator(ULocale.US);
            }
        });
    }

    private final ULocale locale;

    private final UCaseProps csp;
    private int caseLocale;

    /**
     * Constructs a transliterator.
     */
    public UppercaseTransliterator(ULocale loc) {
        super(_ID, null);
        locale = loc;
        csp=UCaseProps.INSTANCE;
        caseLocale = UCaseProps.getCaseLocale(locale);
    }

    /**
     * Implements {@link Transliterator#handleTransliterate}.
     */
    @Override
    protected void handleTransliterate(Replaceable text,
                Position offsets, boolean isIncremental) {
        if(csp==null) {
            return;
        }

        if(offsets.start >= offsets.limit) {
            return;
        }

        ReplaceableContextIterator iter = new ReplaceableContextIterator();
        StringBuilder result = new StringBuilder();

        iter.setText(text);
        int c, delta;

        // Walk through original string
        // If there is a case change, modify corresponding position in replaceable

        iter.setIndex(offsets.start);
        iter.setLimit(offsets.limit);
        iter.setContextLimits(offsets.contextStart, offsets.contextLimit);
        while((c=iter.nextCaseMapCP())>=0) {
            c=csp.toFullUpper(c, iter, result, caseLocale);

            if(iter.didReachLimit() && isIncremental) {
                // the case mapping function tried to look beyond the context limit
                // wait for more input
                offsets.start=iter.getCaseMapCPStart();
                return;
            }

            /* decode the result */
            if(c<0) {
                /* c mapped to itself, no change */
                continue;
            } else if(c<=UCaseProps.MAX_STRING_LENGTH) {
                /* replace by the mapping string */
                delta=iter.replace(result.toString());
                result.setLength(0);
            } else {
                /* replace by single-code point mapping */
                delta=iter.replace(UTF16.valueOf(c));
            }

            if(delta!=0) {
                offsets.limit += delta;
                offsets.contextLimit += delta;
            }
        }
        offsets.start = offsets.limit;
    }

    // NOTE: normally this would be static, but because the results vary by locale....
    SourceTargetUtility sourceTargetUtility = null;

    /* (non-Javadoc)
     * @see com.ibm.icu.text.Transliterator#addSourceTargetSet(com.ibm.icu.text.UnicodeSet, com.ibm.icu.text.UnicodeSet, com.ibm.icu.text.UnicodeSet)
     */
    @Override
    public void addSourceTargetSet(UnicodeSet inputFilter, UnicodeSet sourceSet, UnicodeSet targetSet) {
        synchronized (this) {
            if (sourceTargetUtility == null) {
                sourceTargetUtility = new SourceTargetUtility(new Transform<String,String>() {
                    @Override
                    public String transform(String source) {
                        return UCharacter.toUpperCase(locale, source);
                    }
                });
            }
        }
        sourceTargetUtility.addSourceTargetSet(this, inputFilter, sourceSet, targetSet);
    }
}

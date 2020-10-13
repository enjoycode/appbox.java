package appbox.design.idea;

import com.intellij.codeInsight.completion.*;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.patterns.ElementPattern;
import com.intellij.util.Consumer;

public final class IdeaCompletionResultSet extends CompletionResultSet {

    private static final Logger LOG = Logger.getInstance(BaseCompletionService.class);

    protected final CompletionParameters    myParameters;
    protected       CompletionSorter        mySorter;
    protected final IdeaCompletionResultSet myOriginal;

    public IdeaCompletionResultSet(Consumer<? super CompletionResult> consumer, PrefixMatcher prefixMatcher,
                                   CompletionContributor contributor, CompletionParameters parameters,
                                   CompletionSorter sorter, IdeaCompletionResultSet original) {
        super(prefixMatcher, consumer, contributor);
        myParameters = parameters;
        mySorter     = sorter;
        myOriginal   = original;
    }

    @Override
    public void addElement(LookupElement element) {
        //ProgressManager.checkCanceled();
        if (!element.isValid()) {
            LOG.error("Invalid lookup element: " + element + " of " + element.getClass() +
                    " in " + myParameters.getOriginalFile() + " of " + myParameters.getOriginalFile().getClass());
            return;
        }

        mySorter = mySorter == null ?
                CompletionService.getCompletionService()/*getCompletionService()*/
                        .defaultSorter(myParameters, getPrefixMatcher()) : mySorter;

        CompletionResult matched = CompletionResult.wrap(element, getPrefixMatcher(), mySorter);
        if (matched != null) {
            element.putUserData(BaseCompletionService.LOOKUP_ELEMENT_CONTRIBUTOR, myContributor);
            passResult(matched);
        }
    }

    @Override
    public CompletionResultSet withPrefixMatcher(PrefixMatcher matcher) {
        if (matcher.equals(getPrefixMatcher())) {
            return this;
        }
        return new IdeaCompletionResultSet(getConsumer(), matcher, myContributor, myParameters, mySorter, this);
    }

    @Override
    public CompletionResultSet withPrefixMatcher(String prefix) {
        return withPrefixMatcher(getPrefixMatcher().cloneWithPrefix(prefix));
    }

    @Override
    public void stopHere() {
        if (LOG.isTraceEnabled()) {
            LOG.trace("Completion stopped\n" /*+ DebugUtil.currentStackTrace()*/);
        }
        super.stopHere();
        if (myOriginal != null) {
            myOriginal.stopHere();
        }
    }

    @Override
    public CompletionResultSet withRelevanceSorter(CompletionSorter sorter) {
        return new IdeaCompletionResultSet(getConsumer(), getPrefixMatcher(), myContributor, myParameters, sorter, this);
    }

    @Override
    public void addLookupAdvertisement(String text) {
        //getCompletionService().setAdvertisementText(text);
        throw new RuntimeException();
    }

    @Override
    public CompletionResultSet caseInsensitive() {
        //PrefixMatcher matcher      = getPrefixMatcher();
        //boolean       typoTolerant = matcher instanceof CamelHumpMatcher && ((CamelHumpMatcher) matcher).isTypoTolerant();
        //return withPrefixMatcher(createMatcher(matcher.getPrefix(), false, typoTolerant));
        return this; //TODO:
    }

    @Override
    public void restartCompletionOnPrefixChange(ElementPattern<String> prefixCondition) {
    }

    @Override
    public void restartCompletionWhenNothingMatches() {
    }

}

package de.tum.in.www1.artemis.config;

import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import de.tum.in.www1.artemis.domain.StaticCodeAnalysisDefaultCategory;
import de.tum.in.www1.artemis.domain.enumeration.CategoryState;
import de.tum.in.www1.artemis.domain.enumeration.ProgrammingLanguage;
import de.tum.in.www1.artemis.domain.enumeration.StaticCodeAnalysisTool;

/**
 * Provides hard-coded programming language specific static code analysis default categories as an unmodifiable Map
 */
@Configuration
public class StaticCodeAnalysisConfigurer {

    private final Logger log = LoggerFactory.getLogger(StaticCodeAnalysisConfigurer.class);

    private Map<ProgrammingLanguage, List<StaticCodeAnalysisDefaultCategory>> languageToDefaultCategories;

    public StaticCodeAnalysisConfigurer() {
    }

    @PostConstruct
    private void init() {
        languageToDefaultCategories = Map.of(ProgrammingLanguage.JAVA, createDefaultCategoriesForJava());
        log.debug("Initialized default static code analysis categories for JAVA");
    }

    /**
     * Create an unmodifiable List of default static code analysis categories for Java
     *
     * @return unmodifiable static code analysis categories
     */
    private List<StaticCodeAnalysisDefaultCategory> createDefaultCategoriesForJava() {
        return List.of(
                new StaticCodeAnalysisDefaultCategory("Bad Practice", 0.5D, 5D, CategoryState.FEEDBACK,
                        List.of(createMapping(StaticCodeAnalysisTool.SPOTBUGS, "BAD_PRACTICE"), createMapping(StaticCodeAnalysisTool.PMD, "Best-Practices"))),
                new StaticCodeAnalysisDefaultCategory("Code Style", 0.2D, 2D, CategoryState.FEEDBACK,
                        List.of(createMapping(StaticCodeAnalysisTool.SPOTBUGS, "STYLE"), createMapping(StaticCodeAnalysisTool.CHECKSTYLE, "blocks"),
                                createMapping(StaticCodeAnalysisTool.CHECKSTYLE, "coding"), createMapping(StaticCodeAnalysisTool.CHECKSTYLE, "modifier"),
                                createMapping(StaticCodeAnalysisTool.PMD, "Code-Style"))),
                new StaticCodeAnalysisDefaultCategory("Potential Bugs", 0.5D, 5D, CategoryState.FEEDBACK,
                        List.of(createMapping(StaticCodeAnalysisTool.SPOTBUGS, "CORRECTNESS"), createMapping(StaticCodeAnalysisTool.SPOTBUGS, "MT_CORRECTNESS"),
                                createMapping(StaticCodeAnalysisTool.PMD, "Error Prone"), createMapping(StaticCodeAnalysisTool.PMD, "Multithreading"))),
                new StaticCodeAnalysisDefaultCategory("Security", 2.5D, 10D, CategoryState.FEEDBACK,
                        List.of(createMapping(StaticCodeAnalysisTool.SPOTBUGS, "MALICIOUS_CODE"), createMapping(StaticCodeAnalysisTool.SPOTBUGS, "SECURITY"),
                                createMapping(StaticCodeAnalysisTool.PMD, "Security"))),
                new StaticCodeAnalysisDefaultCategory("Performance", 1D, 2D, CategoryState.FEEDBACK,
                        List.of(createMapping(StaticCodeAnalysisTool.SPOTBUGS, "PERFORMANCE"), createMapping(StaticCodeAnalysisTool.PMD, "Performance"))),
                new StaticCodeAnalysisDefaultCategory("Design", 5D, 5D, CategoryState.FEEDBACK,
                        List.of(createMapping(StaticCodeAnalysisTool.CHECKSTYLE, "design"), createMapping(StaticCodeAnalysisTool.PMD, "Design"))),
                new StaticCodeAnalysisDefaultCategory("Code Metrics", 0D, 0D, CategoryState.INACTIVE,
                        List.of(createMapping(StaticCodeAnalysisTool.CHECKSTYLE, "metrics"), createMapping(StaticCodeAnalysisTool.CHECKSTYLE, "sizes"))),
                new StaticCodeAnalysisDefaultCategory("Documentation", 0D, 0D, CategoryState.INACTIVE,
                        List.of(createMapping(StaticCodeAnalysisTool.SPOTBUGS, "I18N"), createMapping(StaticCodeAnalysisTool.CHECKSTYLE, "javadoc"),
                                createMapping(StaticCodeAnalysisTool.CHECKSTYLE, "annotation"), createMapping(StaticCodeAnalysisTool.PMD, "Documentation"))),
                new StaticCodeAnalysisDefaultCategory("Naming & Formatting", 0D, 0D, CategoryState.INACTIVE,
                        List.of(createMapping(StaticCodeAnalysisTool.CHECKSTYLE, "imports"), createMapping(StaticCodeAnalysisTool.CHECKSTYLE, "indentation"),
                                createMapping(StaticCodeAnalysisTool.CHECKSTYLE, "naming"), createMapping(StaticCodeAnalysisTool.CHECKSTYLE, "whitespaces"))),
                new StaticCodeAnalysisDefaultCategory("Miscellaneous", 0D, 0D, CategoryState.INACTIVE, List.of(createMapping(StaticCodeAnalysisTool.CHECKSTYLE, "miscellaneous"))));
    }

    @Bean(name = "staticCodeAnalysisConfiguration")
    public Map<ProgrammingLanguage, List<StaticCodeAnalysisDefaultCategory>> staticCodeAnalysisConfiguration() {
        return languageToDefaultCategories;
    }

    private StaticCodeAnalysisDefaultCategory.CategoryMapping createMapping(StaticCodeAnalysisTool tool, String category) {
        return new StaticCodeAnalysisDefaultCategory.CategoryMapping(tool, category);
    }
}

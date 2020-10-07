const esModules = ['ngx-treeview', 'lodash-es'].join('|');
module.exports = {
    globals: {
        'ts-jest': {
            tsConfig: '<rootDir>/tsconfig.spec.json',
            stringifyContentPathRegex: '\\.html$',
            astTransformers: {
                before: [require.resolve('./InlineHtmlStripStylesTransformer')],
            },
            diagnostics: {
                ignoreCodes: [151001],
            },
        },
    },
    coverageThreshold: {
        global: {
            branches: 32,
            functions: 40,
            lines: 59,
            // TODO: in the future, the following value should be increase to 80%
            statements: 60,
        },
    },
    preset: 'jest-preset-angular',
    setupFilesAfterEnv: ['<rootDir>/src/test/javascript/jest.ts'],
    modulePaths: ['<rootDir>/src/main/webapp/'],
    transformIgnorePatterns: [`/node_modules/(?!${esModules})`],
    rootDir: '../../../',
    testMatch: [
        '<rootDir>/src/test/javascript/spec/component/**/*.ts',
        '<rootDir>/src/test/javascript/spec/directive/**/*.ts',
        '<rootDir>/src/test/javascript/spec/integration/**/*.ts',
        '<rootDir>/src/test/javascript/spec/pipe/**/*.ts',
        '<rootDir>/src/test/javascript/spec/service/**/*.ts',
        '<rootDir>/src/test/javascript/spec/util/**/*.ts',
    ],
    moduleNameMapper: {
        '^app/(.*)': '<rootDir>/src/main/webapp/app/$1',
        'test/(.*)': '<rootDir>/src/test/javascript/spec/$1',
        '@assets/(.*)': '<rootDir>/src/main/webapp/assets/$1',
        '@core/(.*)': '<rootDir>/src/main/webapp/app/core/$1',
        '@env': '<rootDir>/src/main/webapp/environments/environment',
        '@src/(.*)': '<rootDir>/src/src/$1',
        '@state/(.*)': '<rootDir>/src/app/state/$1',
    },
};

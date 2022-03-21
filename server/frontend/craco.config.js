const CracoEsbuildPlugin = require('craco-esbuild');

module.exports = {
    plugins: [
        {
            plugin: CracoEsbuildPlugin,
            options: {
                esbuildLoaderOptions: {
                    loader: 'tsx',
                    target: 'es2015',
                },
                esbuildMinimizerOptions: {
                    target: 'es2015',
                    css: true,
                },
            },
        },
    ],
};

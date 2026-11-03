// Configuración plana de ESLint con las reglas de Expo (React, hooks e imports).
const { defineConfig } = require('eslint/config');
const expoConfig = require('eslint-config-expo/flat');

module.exports = defineConfig([
  expoConfig,
  {
    // esquema.d.ts lo genera openapi-typescript; no se revisa a mano.
    ignores: ['dist/*', '.expo/*', 'src/api/esquema.d.ts'],
  },
]);

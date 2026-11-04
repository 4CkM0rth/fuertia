/** @type {import('jest').Config} */
module.exports = {
  // jest-expo simula los módulos nativos de Expo y React Native para correr en Node.
  preset: 'jest-expo',
  setupFiles: ['./pruebas/preparar.ts'],
  // Las pruebas viven fuera de src/app porque Expo Router trataría cada archivo de ahí como ruta.
  testMatch: ['<rootDir>/pruebas/**/*.test.ts?(x)'],
};

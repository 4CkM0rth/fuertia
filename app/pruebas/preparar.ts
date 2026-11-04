// AsyncStorage es un módulo nativo: en las pruebas se usa el simulador en memoria que trae el paquete.
jest.mock('@react-native-async-storage/async-storage', () =>
  require('@react-native-async-storage/async-storage/jest/async-storage-mock'),
);

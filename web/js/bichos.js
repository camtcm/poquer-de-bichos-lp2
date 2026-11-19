export const BICHOS = [
  { id: "arana",      nombre: "Araña",      una: "una araña",      plural: "arañas" },
  { id: "cucaracha",  nombre: "Cucaracha",  una: "una cucaracha",  plural: "cucarachas" },
  { id: "sapo",       nombre: "Sapo",       una: "un sapo",        plural: "sapos" },
  { id: "mosca",      nombre: "Mosca",      una: "una mosca",      plural: "moscas" },
  { id: "rata",       nombre: "Rata",       una: "una rata",       plural: "ratas" },
  { id: "escorpion",  nombre: "Escorpión",  una: "un escorpión",   plural: "escorpiones" },
  { id: "murcielago", nombre: "Murciélago", una: "un murciélago",  plural: "murciélagos" },
  { id: "chinche",    nombre: "Chinche",    una: "una chinche",    plural: "chinches" },
];

export function bicho(id) {
  return BICHOS.find((b) => b.id === id);
}

export function imagenDe(id) {
  return `img/bichos/${id}.svg`;
}

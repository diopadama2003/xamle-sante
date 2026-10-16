export const SYMPTOMES = [
  { code: 'FIEVRE', libelle: 'Fièvre' },
  { code: 'DIARRHEE', libelle: 'Diarrhée' },
  { code: 'TOUX', libelle: 'Toux' },
  { code: 'VOMISSEMENTS', libelle: 'Vomissements' },
  { code: 'ERUPTION_CUTANEE', libelle: 'Éruption cutanée' },
  { code: 'DIFFICULTES_RESPIRATOIRES', libelle: 'Difficultés respiratoires' },
]

export function libelleSymptome(code) {
  return SYMPTOMES.find((s) => s.code === code)?.libelle ?? code
}
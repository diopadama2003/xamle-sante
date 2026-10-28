import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import api from '../api'
import { useAuth } from '../AuthContext'
import { libelleSymptome } from '../symptomes'

const NIVEAUX = { FAIBLE: 'Faible', MODERE: 'Modéré', ELEVE: 'Élevé' }
const COULEURS = { FAIBLE: '#f1c40f', MODERE: '#e67e22', ELEVE: '#c0392b' }

function dateFr(iso) {
  return iso.split('-').reverse().join('/')
}

function Barres({ lignes, formater = (x) => x }) {
  if (lignes.length === 0) {
    return <div className="alerte attente">Aucune donnée sur la période.</div>
  }
  const max = Math.max(1, ...lignes.map((l) => l.total))
  return (
    <div>
      {lignes.map((l) => (
        <div key={l.cle} className="barre-ligne">
          <span className="barre-nom">{formater(l.cle)}</span>
          <div className="barre-fond">
            <div className="barre" style={{ width: `${(l.total / max) * 100}%` }} />
          </div>
          <span className="barre-valeur">{l.total}</span>
        </div>
      ))}
    </div>
  )
}

function Evolution({ jours }) {
  const max = Math.max(1, ...jours.map((j) => j.total))
  return (
    <div>
      <div className="evolution">
        {jours.map((j) => (
          <div
            key={j.jour}
            className="evolution-barre"
            title={`${dateFr(j.jour)} : ${j.total}`}
            style={{ height: `${(j.total / max) * 100}%` }}
          />
        ))}
      </div>
      <div className="evolution-legende">
        <span>{dateFr(jours[0].jour)}</span>
        <span>{dateFr(jours[jours.length - 1].jour)}</span>
      </div>
    </div>
  )
}

function Chiffre({ valeur, libelle }) {
  return (
    <div className="carte-stat">
      <div className="carte-stat-valeur">{valeur}</div>
      <div className="carte-stat-libelle">{libelle}</div>
    </div>
  )
}

export default function DashboardPage() {
  const { deconnecter } = useAuth()
  const navigate = useNavigate()
  const [jours, setJours] = useState(30)
  const [stats, setStats] = useState(null)
  const [erreur, setErreur] = useState('')
  const [telechargement, setTelechargement] = useState(false)

  function sessionExpiree() {
    deconnecter()
    navigate('/connexion')
  }

  useEffect(() => {
    let annule = false
    api
      .get('/admin/statistiques', { params: { jours } })
      .then(({ data }) => {
        if (annule) return
        setStats(data)
        setErreur('')
      })
      .catch((err) => {
        if (annule) return
        const code = err.response?.status
        if (code === 401 || code === 403) sessionExpiree()
        else setErreur('Impossible de joindre le serveur')
      })
    return () => {
      annule = true
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [jours])

  async function exporter() {
    setErreur('')
    setTelechargement(true)
    try {
      const { data } = await api.get('/admin/statistiques/export.csv', {
        params: { jours },
        responseType: 'blob',
      })
      const url = window.URL.createObjectURL(data)
      const lien = document.createElement('a')
      lien.href = url
      lien.download = `signalements-${jours}j.csv`
      document.body.appendChild(lien)
      lien.click()
      lien.remove()
      window.URL.revokeObjectURL(url)
    } catch (err) {
      const code = err.response?.status
      if (code === 401 || code === 403) sessionExpiree()
      else setErreur("L'export a échoué")
    } finally {
      setTelechargement(false)
    }
  }

  const p = stats?.plateforme

  return (
    <div className="page">
      <div className="formulaire xlarge">
        <h2>Tableau de bord santé publique</h2>

        <div className="outils">
          <label>
            Période
            <select value={jours} onChange={(e) => setJours(Number(e.target.value))}>
              <option value={7}>7 derniers jours</option>
              <option value={14}>14 derniers jours</option>
              <option value={30}>30 derniers jours</option>
              <option value={90}>90 derniers jours</option>
            </select>
          </label>
          <button className="bouton" onClick={exporter} disabled={telechargement}>
            {telechargement ? 'Export...' : 'Exporter en CSV'}
          </button>
        </div>

        {erreur && <div className="alerte erreur">{erreur}</div>}
        {!stats && !erreur && <div>Chargement...</div>}

        {stats && (
          <>
            <div className="cartes-stats">
              <Chiffre valeur={stats.signalementsValides} libelle="Signalements validés" />
              <Chiffre valeur={stats.signalementsEnAttente} libelle="À modérer" />
              <Chiffre valeur={stats.zonesARisque.length} libelle="Zones à risque" />
              <Chiffre valeur={p.alertesSosSemaine} libelle="Alertes SOS (7 j)" />
            </div>

            <h3>Évolution des signalements</h3>
            <Evolution jours={stats.parJour} />

            <h3>Zones à risque</h3>
            {stats.zonesARisque.length === 0 && (
              <div className="alerte ok">Aucune zone n'atteint le seuil de 3 signalements.</div>
            )}
            {stats.zonesARisque.map((z) => (
              <div key={z.quartier} className="carte-profil">
                <div>
                  <strong>{z.quartier}</strong> · {z.total} signalements ·{' '}
                  <span style={{ color: COULEURS[z.niveau], fontWeight: 600 }}>
                    {NIVEAUX[z.niveau]}
                  </span>
                </div>
              </div>
            ))}

            <h3>Par symptôme</h3>
            <Barres lignes={stats.parSymptome} formater={libelleSymptome} />

            <h3>Par quartier</h3>
            <Barres lignes={stats.parQuartier} />

            <h3>Plateforme</h3>
            <div className="cartes-stats">
              <Chiffre valeur={p.patients} libelle="Patients" />
              <Chiffre valeur={p.professionnels} libelle="Professionnels" />
              <Chiffre valeur={p.specialistesValides} libelle="Spécialistes validés" />
              <Chiffre valeur={p.specialistesEnAttente} libelle="Spécialistes en attente" />
              <Chiffre valeur={p.rendezVousConfirmes} libelle="RDV confirmés" />
              <Chiffre valeur={p.rendezVousAnnules} libelle="RDV annulés" />
              <Chiffre valeur={p.campagnesAVenir} libelle="Campagnes à venir" />
            </div>

            <p className="note">
              Données épidémiologiques anonymes : aucun nom, aucun numéro, aucune position précise.
              L'export CSV contient uniquement la date, le symptôme et le quartier.
            </p>
          </>
        )}
      </div>
    </div>
  )
}
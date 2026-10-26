import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import api from '../api'
import { useAuth } from '../AuthContext'

const LIBELLES = {
  EN_ATTENTE_PAIEMENT: 'En attente de paiement',
  CONFIRME: 'Confirmé',
  ANNULE: 'Annulé',
}

function formater(iso) {
  return new Date(iso).toLocaleString('fr-FR', { dateStyle: 'medium', timeStyle: 'short' })
}

export default function AgendaPro() {
  const { deconnecter } = useAuth()
  const navigate = useNavigate()
  const [creneaux, setCreneaux] = useState([])
  const [rdvs, setRdvs] = useState([])
  const [debut, setDebut] = useState('')
  const [fin, setFin] = useState('')
  const [message, setMessage] = useState('')
  const [erreur, setErreur] = useState('')
  const [chargement, setChargement] = useState(true)

  function sessionExpiree() {
    deconnecter()
    navigate('/connexion')
  }

  function charger() {
    return Promise.all([
      api.get('/professionnel/creneaux'),
      api.get('/professionnel/rendez-vous'),
    ])
      .then(([c, r]) => {
        setCreneaux(c.data)
        setRdvs(r.data)
      })
      .catch((err) => {
        const code = err.response?.status
        if (code === 401 || code === 403) sessionExpiree()
        else if (code === 404) setErreur("Crée d'abord ton profil dans « Mon profil »")
        else setErreur('Impossible de joindre le serveur')
      })
      .finally(() => setChargement(false))
  }

  useEffect(() => {
    charger()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  async function creer(e) {
    e.preventDefault()
    setErreur('')
    setMessage('')
    try {
      await api.post('/professionnel/creneaux', { debut, fin })
      setDebut('')
      setFin('')
      setMessage('Créneau publié.')
      charger()
    } catch (err) {
      const code = err.response?.status
      if (code === 401) sessionExpiree()
      else if (code === 403) setErreur('Ton profil doit être validé par un modérateur avant de publier des créneaux')
      else if (code === 404) setErreur("Crée d'abord ton profil dans « Mon profil »")
      else if (code === 409) setErreur('Ce créneau chevauche un créneau existant')
      else if (code === 400) setErreur('Dates invalides : dans le futur, durée de 15 à 120 minutes')
      else setErreur('Impossible de joindre le serveur')
    }
  }

  async function supprimerCreneau(c) {
    setErreur('')
    setMessage('')
    try {
      await api.delete(`/professionnel/creneaux/${c.id}`)
      setMessage('Créneau supprimé.')
      charger()
    } catch (err) {
      const code = err.response?.status
      if (code === 401) sessionExpiree()
      else if (code === 409) setErreur('Ce créneau a un rendez-vous ou un historique')
      else setErreur('La suppression a échoué')
    }
  }

  async function annulerRdv(r) {
    if (!window.confirm('Annuler ce rendez-vous ?')) return
    setErreur('')
    setMessage('')
    try {
      await api.patch(`/professionnel/rendez-vous/${r.id}/annuler`)
      setMessage('Rendez-vous annulé. Le patient est remboursé (simulation).')
      charger()
    } catch (err) {
      const code = err.response?.status
      if (code === 401) sessionExpiree()
      else if (code === 409) setErreur('Ce rendez-vous est déjà annulé')
      else if (code === 400) setErreur('Ce rendez-vous est déjà passé')
      else setErreur("L'annulation a échoué")
    }
  }

  if (chargement) return <div className="page">Chargement...</div>

  return (
    <div className="page">
      <form className="formulaire large" onSubmit={creer}>
        <h2>Publier un créneau</h2>
        <label>
          Début
          <input type="datetime-local" required value={debut} onChange={(e) => setDebut(e.target.value)} />
        </label>
        <label>
          Fin
          <input type="datetime-local" required value={fin} onChange={(e) => setFin(e.target.value)} />
        </label>
        {message && <div className="alerte ok">{message}</div>}
        {erreur && <div className="alerte erreur">{erreur}</div>}
        <button className="bouton" type="submit">
          Publier le créneau
        </button>
      </form>

      <div className="formulaire large" style={{ marginTop: 16 }}>
        <h2>Mes créneaux à venir</h2>
        {creneaux.length === 0 && <div className="alerte attente">Aucun créneau publié.</div>}
        {creneaux.map((c) => (
          <div key={c.id} className="carte-profil">
            <div>
              {formater(c.debut)} → {new Date(c.fin).toLocaleTimeString('fr-FR', { timeStyle: 'short' })}
            </div>
            <div>
              <span className={`statut ${c.reserve ? 'CONFIRME' : 'LIBRE'}`}>
                {c.reserve ? 'Réservé' : 'Libre'}
              </span>
            </div>
            {!c.reserve && (
              <div className="actions">
                <button type="button" className="bouton danger" onClick={() => supprimerCreneau(c)}>
                  Supprimer
                </button>
              </div>
            )}
          </div>
        ))}
      </div>

      <div className="formulaire large" style={{ marginTop: 16 }}>
        <h2>Mes rendez-vous</h2>
        {rdvs.length === 0 && <div className="alerte attente">Aucun rendez-vous.</div>}
        {rdvs.map((r) => (
          <div key={r.id} className="carte-profil">
            <div>
              <strong>{r.patientNom}</strong> · {formater(r.debut)}
            </div>
            {r.motif && <div className="detail">Motif : {r.motif}</div>}
            <div>
              <span className={`statut ${r.statut}`}>{LIBELLES[r.statut]}</span>
            </div>
            <div className="actions">
              {r.statut === 'CONFIRME' && r.lienVisio && (
                <a className="bouton" href={r.lienVisio} target="_blank" rel="noreferrer">
                  Ouvrir la visio
                </a>
              )}
              {r.statut !== 'ANNULE' && new Date(r.debut) > new Date() && (
                <button type="button" className="bouton danger" onClick={() => annulerRdv(r)}>
                  Annuler
                </button>
              )}
            </div>
          </div>
        ))}
      </div>
    </div>
  )
}
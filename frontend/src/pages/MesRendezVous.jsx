import { useEffect, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import api from '../api'
import { useAuth } from '../AuthContext'

const LIBELLES = {
  EN_ATTENTE_PAIEMENT: 'En attente de paiement',
  CONFIRME: 'Confirmé',
  ANNULE: 'Annulé',
}

function formater(iso) {
  return new Date(iso).toLocaleString('fr-FR', { dateStyle: 'full', timeStyle: 'short' })
}

export default function MesRendezVous() {
  const { deconnecter } = useAuth()
  const navigate = useNavigate()
  const [liste, setListe] = useState([])
  const [chargement, setChargement] = useState(true)
  const [message, setMessage] = useState('')
  const [erreur, setErreur] = useState('')

  function sessionExpiree() {
    deconnecter()
    navigate('/connexion')
  }

  function charger() {
    api
      .get('/patient/rendez-vous')
      .then(({ data }) => setListe(data))
      .catch((err) => {
        const code = err.response?.status
        if (code === 401 || code === 403) sessionExpiree()
        else setErreur('Impossible de joindre le serveur')
      })
      .finally(() => setChargement(false))
  }

  useEffect(() => {
    charger()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  async function annuler(r) {
    if (!window.confirm('Annuler ce rendez-vous ?')) return
    setMessage('')
    setErreur('')
    try {
      await api.patch(`/patient/rendez-vous/${r.id}/annuler`)
      setMessage(
        r.paiementStatut === 'PAYE'
          ? 'Rendez-vous annulé. Ton paiement est remboursé (simulation).'
          : 'Rendez-vous annulé.',
      )
      charger()
    } catch (err) {
      const code = err.response?.status
      if (code === 401 || code === 403) sessionExpiree()
      else if (code === 409) setErreur('Ce rendez-vous est déjà annulé')
      else if (code === 400) setErreur('Ce rendez-vous est déjà passé')
      else setErreur("L'annulation a échoué")
    }
  }

  if (chargement) return <div className="page">Chargement...</div>

  return (
    <div className="page">
      <div className="formulaire large">
        <h2>Mes rendez-vous</h2>

        {message && <div className="alerte ok">{message}</div>}
        {erreur && <div className="alerte erreur">{erreur}</div>}

        {liste.length === 0 && !erreur && (
          <div className="alerte attente">
            Aucun rendez-vous. Cherche un spécialiste sur la <Link to="/">carte</Link>.
          </div>
        )}

        {liste.map((r) => (
          <div key={r.id} className="carte-profil">
            <div>
              <strong>{r.specialisteNom}</strong> · {r.specialite}
            </div>
            <div className="detail">{formater(r.debut)}</div>
            {r.motif && <div className="detail">Motif : {r.motif}</div>}
            <div>
              <span className={`statut ${r.statut}`}>{LIBELLES[r.statut]}</span>
            </div>

            <div className="actions">
              {r.statut === 'EN_ATTENTE_PAIEMENT' && r.paiementReference && (
                <Link className="bouton" to={`/paiement/${r.paiementReference}`}>
                  Payer
                </Link>
              )}
              {r.statut === 'CONFIRME' && r.lienVisio && (
                <a className="bouton" href={r.lienVisio} target="_blank" rel="noreferrer">
                  Rejoindre la visio
                </a>
              )}
              {r.statut !== 'ANNULE' && new Date(r.debut) > new Date() && (
                <button className="bouton danger" onClick={() => annuler(r)}>
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
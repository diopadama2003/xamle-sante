import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import api from '../api'
import { useAuth } from '../AuthContext'
import { ChoixEtoiles } from './Etoiles'
import '../avis.css'

function dateCourte(iso) {
  return iso ? new Date(iso).toLocaleDateString('fr-FR') : ''
}

function texteErreur(err) {
  const code = err.response?.status
  if (code === 403) return 'Tu pourras donner ton avis après un rendez-vous payé avec ce médecin.'
  if (code === 401) return 'Session expirée : reconnecte-toi.'
  if (code === 400) return 'Vérifie la note (1 à 5) et ton commentaire (500 caractères maximum).'
  if (code === 404) return "Ce médecin n'est plus référencé."
  return 'Impossible de joindre le serveur.'
}

// Liste des avis d'un médecin + formulaire pour le patient connecté
export default function AvisSection({ specialisteId, nombre = 0, libelle, onChange }) {
  const { user } = useAuth()
  const estPatient = user?.role === 'PATIENT'
  const [ouvert, setOuvert] = useState(false)
  const [liste, setListe] = useState(null)
  const [mon, setMon] = useState(null)
  const [note, setNote] = useState(0)
  const [commentaire, setCommentaire] = useState('')
  const [envoi, setEnvoi] = useState(false)
  const [message, setMessage] = useState('')
  const [erreur, setErreur] = useState('')

  function charger() {
    api
      .get(`/specialistes/${specialisteId}/avis`)
      .then(({ data }) => setListe(data))
      .catch(() => setErreur('Impossible de charger les avis.'))
    if (estPatient) {
      api
        .get(`/patient/specialistes/${specialisteId}/avis/moi`)
        .then(({ data }) => {
          setMon(data)
          setNote(data.avis?.note ?? 0)
          setCommentaire(data.avis?.commentaire ?? '')
        })
        .catch(() => {})
    }
  }

  useEffect(() => {
    if (ouvert) charger()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [ouvert, specialisteId])

  async function envoyer(e) {
    e.preventDefault()
    setMessage('')
    setErreur('')
    if (note < 1) {
      setErreur('Choisis une note de 1 à 5 étoiles.')
      return
    }
    setEnvoi(true)
    try {
      await api.put(`/patient/specialistes/${specialisteId}/avis`, { note, commentaire })
      setMessage(mon?.avis ? 'Ton avis a été modifié.' : 'Merci pour ton avis !')
      charger()
      if (onChange) onChange()
    } catch (err) {
      setErreur(texteErreur(err))
    } finally {
      setEnvoi(false)
    }
  }

  async function supprimer() {
    setMessage('')
    setErreur('')
    setEnvoi(true)
    try {
      await api.delete(`/patient/specialistes/${specialisteId}/avis`)
      setNote(0)
      setCommentaire('')
      setMessage('Ton avis a été supprimé.')
      charger()
      if (onChange) onChange()
    } catch (err) {
      setErreur(texteErreur(err))
    } finally {
      setEnvoi(false)
    }
  }

  const titre = libelle || (nombre > 0 ? `Voir les avis (${nombre})` : 'Avis des patients')

  return (
    <div className="avis-section">
      <button
        type="button"
        className="avis-bascule"
        onClick={() => setOuvert((o) => !o)}
        aria-expanded={ouvert}
      >
        {ouvert ? 'Masquer les avis' : titre}
      </button>

      {ouvert && (
        <div className="avis-corps">
          {liste && liste.length === 0 && <p className="avis-vide">Aucun avis pour le moment.</p>}
          {liste && liste.length > 0 && (
            <ul className="avis-liste">
              {liste.map((a) => (
                <li key={a.id}>
                  <div className="avis-ligne">
                    <span
                      className="etoiles"
                      style={{ '--pct': `${a.note * 20}%` }}
                      aria-label={`${a.note} sur 5`}
                    >
                      ★★★★★
                    </span>
                    <strong>{a.auteur}</strong>
                    <small>{dateCourte(a.date)}</small>
                  </div>
                  {a.commentaire && <p>{a.commentaire}</p>}
                </li>
              ))}
            </ul>
          )}

          {!user && (
            <p className="avis-note">
              Pour donner ton avis, <Link to="/connexion">connecte-toi</Link> avec un compte patient.
            </p>
          )}

          {estPatient && mon && !mon.peutNoter && (
            <p className="avis-note">
              Tu pourras donner ton avis après un rendez-vous payé avec ce médecin.
            </p>
          )}

          {estPatient && mon?.peutNoter && (
            <form className="avis-form" onSubmit={envoyer}>
              <strong>{mon.avis ? 'Modifier mon avis' : 'Donner mon avis'}</strong>
              <ChoixEtoiles valeur={note} onChange={setNote} />
              <textarea
                rows={3}
                maxLength={500}
                placeholder="Ton commentaire (facultatif)"
                value={commentaire}
                onChange={(e) => setCommentaire(e.target.value)}
              />
              <div className="avis-actions">
                <button className="avis-btn" type="submit" disabled={envoi}>
                  {envoi ? 'Envoi...' : mon.avis ? 'Mettre à jour' : 'Publier mon avis'}
                </button>
                {mon.avis && (
                  <button
                    type="button"
                    className="avis-btn contour"
                    disabled={envoi}
                    onClick={supprimer}
                  >
                    Supprimer mon avis
                  </button>
                )}
              </div>
            </form>
          )}

          {message && <div className="avis-msg ok">{message}</div>}
          {erreur && <div className="avis-msg erreur">{erreur}</div>}
        </div>
      )}
    </div>
  )
}
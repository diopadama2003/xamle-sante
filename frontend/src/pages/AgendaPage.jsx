import { useEffect, useState } from 'react'
import api from '../api'

const TYPES = {
  VACCINATION: 'Vaccination',
  DEPISTAGE: 'Dépistage',
  SENSIBILISATION: 'Sensibilisation',
}

function formater(iso) {
  return new Date(iso).toLocaleString('fr-FR', { dateStyle: 'full', timeStyle: 'short' })
}

export default function AgendaPage() {
  const [campagnes, setCampagnes] = useState([])
  const [filtre, setFiltre] = useState('')
  const [ouverte, setOuverte] = useState(null)
  const [telephone, setTelephone] = useState('')
  const [retour, setRetour] = useState(null)
  const [erreur, setErreur] = useState('')
  const [envoi, setEnvoi] = useState(false)

  useEffect(() => {
    api
      .get('/campagnes')
      .then(({ data }) => setCampagnes(data))
      .catch(() => setErreur('Impossible de joindre le serveur'))
  }, [])

  function ouvrir(id) {
    setOuverte(ouverte === id ? null : id)
    setTelephone('')
    setRetour(null)
  }

  async function demanderRappel(e, id) {
    e.preventDefault()
    setRetour(null)
    setEnvoi(true)
    try {
      const { data } = await api.post(`/campagnes/${id}/rappel`, { telephone })
      setRetour({ id, ok: true, texte: data.message })
    } catch (err) {
      const code = err.response?.status
      let texte = 'Impossible de joindre le serveur'
      if (code === 409) texte = 'Tu es déjà inscrit à ce rappel'
      else if (code === 400) texte = 'Numéro invalide, ou campagne déjà commencée'
      else if (code === 429) texte = 'Trop de demandes, réessaie dans quelques minutes'
      setRetour({ id, ok: false, texte })
    } finally {
      setEnvoi(false)
    }
  }

  const affichees = campagnes.filter((c) => !filtre || c.type === filtre)

  return (
    <div className="page">
      <div className="formulaire large">
        <h2>Agenda santé</h2>
        <p className="note">
          Campagnes de vaccination, de dépistage et de sensibilisation à Ziguinchor. Reçois un
          SMS de rappel 24 h avant.
        </p>

        <label>
          Type de campagne
          <select value={filtre} onChange={(e) => setFiltre(e.target.value)}>
            <option value="">Toutes</option>
            {Object.entries(TYPES).map(([code, libelle]) => (
              <option key={code} value={code}>
                {libelle}
              </option>
            ))}
          </select>
        </label>

        {erreur && <div className="alerte erreur">{erreur}</div>}
        {!erreur && affichees.length === 0 && (
          <div className="alerte attente">Aucune campagne à venir.</div>
        )}

        {affichees.map((c) => (
          <div key={c.id} className="carte-profil">
            <div>
              <span className="badge">{TYPES[c.type]}</span> <strong>{c.titre}</strong>
            </div>
            <div className="detail">{formater(c.dateDebut)}</div>
            <div className="detail">
              {c.lieu}
              {c.quartier ? ` · ${c.quartier}` : ''}
            </div>
            {c.description && <div className="detail">{c.description}</div>}

            <div className="actions">
              <button className="bouton" onClick={() => ouvrir(c.id)}>
                Me rappeler par SMS
              </button>
              <a
                className="bouton secondaire"
                target="_blank"
                rel="noreferrer"
                href={`https://www.google.com/maps/dir/?api=1&destination=${c.latitude},${c.longitude}`}
              >
                Itinéraire
              </a>
            </div>

            {ouverte === c.id && (
              <form className="sms-form" onSubmit={(e) => demanderRappel(e, c.id)}>
                <input
                  type="tel"
                  required
                  placeholder="Ton numéro (ex : 77 123 45 67)"
                  value={telephone}
                  onChange={(e) => setTelephone(e.target.value)}
                />
                <button className="bouton" type="submit" disabled={envoi}>
                  {envoi ? 'Envoi...' : 'Activer le rappel'}
                </button>
              </form>
            )}

            {retour?.id === c.id && (
              <div className={`alerte ${retour.ok ? 'ok' : 'erreur'}`}>{retour.texte}</div>
            )}
          </div>
        ))}
      </div>
    </div>
  )
}
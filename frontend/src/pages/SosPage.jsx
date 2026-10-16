import { useState } from 'react'
import api from '../api'

const NUMEROS = [
  { nom: 'SAMU', numero: '1515' },
  { nom: 'Pompiers', numero: '18' },
  { nom: 'Police', numero: '17' },
]

const ZIGUINCHOR = [12.5833, -16.2719]

export default function SosPage() {
  const [position, setPosition] = useState(null)
  const [urgences, setUrgences] = useState([])
  const [etat, setEtat] = useState('repos')
  const [erreur, setErreur] = useState('')
  const [telephone, setTelephone] = useState('')
  const [envoi, setEnvoi] = useState(false)
  const [resultatSms, setResultatSms] = useState(null)

  async function chargerUrgences(pos) {
    try {
      const { data } = await api.get('/etablissements/urgences', {
        params: { lat: pos[0], lng: pos[1] },
      })
      setUrgences(data)
      setPosition(pos)
      setEtat('pret')
      setErreur('')
    } catch {
      setErreur('Impossible de joindre le serveur')
      setEtat('repos')
    }
  }

  function declencher() {
    setErreur('')
    setResultatSms(null)
    if (!navigator.geolocation) {
      setErreur("La géolocalisation n'est pas disponible. Appelle directement un numéro d'urgence.")
      return
    }
    setEtat('localisation')
    navigator.geolocation.getCurrentPosition(
      (p) => chargerUrgences([p.coords.latitude, p.coords.longitude]),
      () => {
        setErreur('Position refusée ou indisponible. Appelle directement un numéro d\'urgence.')
        setEtat('repos')
      },
      { enableHighAccuracy: true, timeout: 8000, maximumAge: 60000 },
    )
  }

  async function envoyerSms(e) {
    e.preventDefault()
    setErreur('')
    setResultatSms(null)
    setEnvoi(true)
    try {
      const { data } = await api.post('/sos', {
        latitude: position[0],
        longitude: position[1],
        telephone,
      })
      setResultatSms(data)
    } catch (err) {
      const code = err.response?.status
      if (code === 429) setErreur('Trop de demandes. Réessaie dans quelques minutes.')
      else if (code === 400) setErreur('Numéro de téléphone invalide')
      else setErreur('Impossible de joindre le serveur')
    } finally {
      setEnvoi(false)
    }
  }

  return (
    <div className="page">
      <div className="formulaire large">
        <h2>Urgence</h2>

        <div className="numeros-urgence">
          {NUMEROS.map((n) => (
            <a key={n.numero} className="bouton danger" href={`tel:${n.numero}`}>
              {n.nom} : {n.numero}
            </a>
          ))}
        </div>

        <button
          className="sos-bouton"
          onClick={declencher}
          disabled={etat === 'localisation'}
        >
          {etat === 'localisation' ? 'Localisation...' : 'SOS'}
        </button>
        <p className="note">
          Appuie pour te localiser et trouver les urgences les plus proches.
        </p>

        <button
          type="button"
          className="bouton secondaire"
          onClick={() => chargerUrgences(ZIGUINCHOR)}
        >
          Centre-ville (test)
        </button>

        {erreur && <div className="alerte erreur">{erreur}</div>}

        {etat === 'pret' && (
          <>
            <h3>Urgences les plus proches</h3>
            {urgences.length === 0 && (
              <div className="alerte attente">Aucune urgence enregistrée.</div>
            )}
            {urgences.map((u) => (
              <div key={u.id} className="carte-profil">
                <div>
                  <strong>{u.nom}</strong>
                  {u.distanceKm != null && (
                    <span className="dist"> · {u.distanceKm.toFixed(1)} km</span>
                  )}
                </div>
                <div className="detail">{u.horaires}</div>
                <div className="actions">
                  {u.telephone && (
                    <a className="bouton" href={`tel:${u.telephone}`}>
                      Appeler
                    </a>
                  )}
                  <a
                    className="bouton secondaire"
                    target="_blank"
                    rel="noreferrer"
                    href={`https://www.google.com/maps/dir/?api=1&destination=${u.latitude},${u.longitude}`}
                  >
                    Itinéraire
                  </a>
                </div>
              </div>
            ))}

            <h3>Prévenir un proche par SMS</h3>
            <form className="sms-form" onSubmit={envoyerSms}>
              <input
                type="tel"
                required
                placeholder="Numéro du proche (ex : 77 123 45 67)"
                value={telephone}
                onChange={(e) => setTelephone(e.target.value)}
              />
              <button className="bouton" type="submit" disabled={envoi}>
                {envoi ? 'Envoi...' : 'Envoyer ma position'}
              </button>
            </form>

            {resultatSms && (
              <div className={`alerte ${resultatSms.smsEnvoye ? 'ok' : 'erreur'}`}>
                {resultatSms.smsEnvoye
                  ? 'SMS envoyé avec ta position.'
                  : "Le SMS n'a pas pu partir. Appelle ton proche directement."}
              </div>
            )}
          </>
        )}
      </div>
    </div>
  )
}
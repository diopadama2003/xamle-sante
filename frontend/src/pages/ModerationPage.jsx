import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import api from '../api'
import { useAuth } from '../AuthContext'
import { libelleSymptome } from '../symptomes'
import AvatarMedecin from '../components/AvatarMedecin'
import Etoiles from '../components/Etoiles'
import '../avis.css'

const norm = (t) =>
  (t || '')
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '')
    .toLowerCase()

export default function ModerationPage() {
  const { user, deconnecter } = useAuth()
  const navigate = useNavigate()
  const estAdmin = user?.role === 'ADMIN_SANTE'
  const [profils, setProfils] = useState([])
  const [signalements, setSignalements] = useState([])
  const [valides, setValides] = useState([])
  const [filtre, setFiltre] = useState('')
  const [aSupprimer, setASupprimer] = useState(null)
  const [bloquer, setBloquer] = useState(false)
  const [suppression, setSuppression] = useState(false)
  const [chargement, setChargement] = useState(true)
  const [erreur, setErreur] = useState('')
  const [message, setMessage] = useState('')

  function sessionExpiree() {
    deconnecter()
    navigate('/connexion')
  }

  useEffect(() => {
    const demandes = [
      api.get('/moderation/specialistes/en-attente'),
      api.get('/moderation/signalements/en-attente'),
    ]
    if (estAdmin) demandes.push(api.get('/specialistes'))
    Promise.all(demandes)
      .then(([p, s, v]) => {
        setProfils(p.data)
        setSignalements(s.data)
        if (v) setValides(v.data)
        setErreur('')
      })
      .catch((err) => {
        const code = err.response?.status
        if (code === 401 || code === 403) sessionExpiree()
        else setErreur('Impossible de joindre le serveur')
      })
      .finally(() => setChargement(false))
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  async function deciderProfil(profil, action) {
    setMessage('')
    try {
      await api.patch(`/moderation/specialistes/${profil.id}/${action}`)
      setProfils((liste) => liste.filter((p) => p.id !== profil.id))
      if (estAdmin && action === 'valider') {
        api
          .get('/specialistes')
          .then(({ data }) => setValides(data))
          .catch(() => {})
      }
      setMessage(
        action === 'valider'
          ? `Dr ${profil.prenom} ${profil.nom} validé : profil public.`
          : `Dr ${profil.prenom} ${profil.nom} rejeté.`,
      )
    } catch (err) {
      const code = err.response?.status
      if (code === 401 || code === 403) sessionExpiree()
      else setErreur("L'action a échoué")
    }
  }

  async function deciderSignalement(sig, action) {
    setMessage('')
    try {
      await api.patch(`/moderation/signalements/${sig.id}/${action}`)
      setSignalements((liste) => liste.filter((s) => s.id !== sig.id))
      setMessage(
        action === 'valider'
          ? 'Signalement validé : il compte dans la carte de chaleur.'
          : 'Signalement rejeté.',
      )
    } catch (err) {
      const code = err.response?.status
      if (code === 401 || code === 403) sessionExpiree()
      else setErreur("L'action a échoué")
    }
  }

  function demanderSuppression(p) {
    setASupprimer(p)
    setBloquer(false)
    setMessage('')
    setErreur('')
  }

  async function confirmerSuppression() {
    const p = aSupprimer
    setSuppression(true)
    setMessage('')
    setErreur('')
    try {
      const { data } = await api.delete(`/admin/specialistes/${p.id}`, {
        params: { bloquerCompte: bloquer },
      })
      setValides((liste) => liste.filter((x) => x.id !== p.id))
      let texte = `Dr ${data.professionnel} a été supprimé.`
      if (data.rendezVousAnnules > 0) {
        texte += ` ${data.rendezVousAnnules} rendez-vous annulé(s) : les patients sont prévenus par SMS.`
      }
      if (data.compteBloque) texte += ' Son compte est bloqué.'
      setMessage(texte)
      setASupprimer(null)
      setBloquer(false)
    } catch (err) {
      const code = err.response?.status
      if (code === 401 || code === 403) {
        sessionExpiree()
      } else if (code === 404) {
        setValides((liste) => liste.filter((x) => x.id !== p.id))
        setASupprimer(null)
        setErreur('Ce professionnel avait déjà été supprimé.')
      } else {
        setErreur('La suppression a échoué.')
      }
    } finally {
      setSuppression(false)
    }
  }

  if (chargement) return <div className="page">Chargement...</div>

  const affiches = valides.filter(
    (p) => !filtre || norm(`${p.prenom} ${p.nom} ${p.specialite}`).includes(norm(filtre)),
  )

  return (
    <div className="page">
      <div className="formulaire large">
        <h2>Profils à valider</h2>

        {message && <div className="alerte ok">{message}</div>}
        {erreur && <div className="alerte erreur">{erreur}</div>}

        {profils.length === 0 && !erreur && (
          <div className="alerte ok">Aucun profil en attente.</div>
        )}

        {profils.map((p) => (
          <div key={p.id} className="carte-profil">
            <div style={{ marginBottom: 8 }}>
              <AvatarMedecin medecin={p} taille={64} />
            </div>
            <div>
              <strong>
                Dr {p.prenom} {p.nom}
              </strong>{' '}
              · {p.specialite}
            </div>
            <div className="detail">
              {p.quartier || 'Quartier non renseigné'} · {p.telephone || 'Sans téléphone'}
            </div>
            <div className="detail">Langues : {p.langues || 'non renseignées'}</div>
            <div className="detail">
              Position : {p.latitude}, {p.longitude}
            </div>
            <div className="actions">
              <button className="bouton" onClick={() => deciderProfil(p, 'valider')}>
                Valider
              </button>
              <button className="bouton danger" onClick={() => deciderProfil(p, 'rejeter')}>
                Rejeter
              </button>
            </div>
          </div>
        ))}

        <h2>Signalements à vérifier</h2>

        {signalements.length === 0 && !erreur && (
          <div className="alerte ok">Aucun signalement en attente.</div>
        )}

        {signalements.map((s) => (
          <div key={s.id} className="carte-profil">
            <div>
              <strong>{libelleSymptome(s.symptome)}</strong> · {s.quartier}
            </div>
            <div className="detail">
              Reçu le {new Date(s.dateCreation).toLocaleString('fr-FR')}
            </div>
            <div className="actions">
              <button className="bouton" onClick={() => deciderSignalement(s, 'valider')}>
                Valider
              </button>
              <button className="bouton danger" onClick={() => deciderSignalement(s, 'rejeter')}>
                Rejeter
              </button>
            </div>
          </div>
        ))}

        {estAdmin && (
          <>
            <h2>Professionnels référencés ({valides.length})</h2>
            <p className="note">
              Un professionnel supprimé disparaît de l'annuaire avec ses créneaux, rendez-vous et
              avis. Les patients ayant un rendez-vous à venir sont prévenus par SMS.
            </p>
            <input
              type="text"
              placeholder="Rechercher un nom ou une spécialité"
              value={filtre}
              onChange={(e) => setFiltre(e.target.value)}
            />

            {affiches.length === 0 && (
              <div className="alerte attente">Aucun professionnel ne correspond.</div>
            )}

            {affiches.map((p) => (
              <div key={p.id} className="carte-profil">
                <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
                  <AvatarMedecin medecin={p} taille={48} />
                  <div style={{ flex: 1, minWidth: 0 }}>
                    <strong>
                      Dr {p.prenom} {p.nom}
                    </strong>{' '}
                    · {p.specialite}
                    <div className="detail">{p.quartier || 'Quartier non renseigné'}</div>
                    <Etoiles note={p.noteMoyenne} nombre={p.nombreAvis} petit />
                  </div>
                </div>

                {aSupprimer?.id === p.id ? (
                  <div className="pro-confirm">
                    <strong>
                      Supprimer définitivement Dr {p.prenom} {p.nom} ?
                    </strong>
                    <label>
                      <input
                        type="checkbox"
                        checked={bloquer}
                        onChange={(e) => setBloquer(e.target.checked)}
                      />
                      Bloquer aussi son compte (il ne pourra plus se connecter)
                    </label>
                    <div className="actions">
                      <button
                        className="bouton danger"
                        disabled={suppression}
                        onClick={confirmerSuppression}
                      >
                        {suppression ? 'Suppression...' : 'Confirmer la suppression'}
                      </button>
                      <button
                        className="bouton secondaire"
                        disabled={suppression}
                        onClick={() => setASupprimer(null)}
                      >
                        Annuler
                      </button>
                    </div>
                  </div>
                ) : (
                  <div className="actions">
                    <button className="bouton danger" onClick={() => demanderSuppression(p)}>
                      Supprimer
                    </button>
                  </div>
                )}
              </div>
            ))}
          </>
        )}
      </div>
    </div>
  )
}
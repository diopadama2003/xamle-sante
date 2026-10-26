import { useEffect, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import api from '../api'
import { useAuth } from '../AuthContext'

function formater(iso) {
  return new Date(iso).toLocaleString('fr-FR', { dateStyle: 'full', timeStyle: 'short' })
}

export default function PaiementPage() {
  const { reference } = useParams()
  const navigate = useNavigate()
  const { deconnecter } = useAuth()
  const [paiement, setPaiement] = useState(null)
  const [erreur, setErreur] = useState('')
  const [envoi, setEnvoi] = useState(false)

  function sessionExpiree() {
    deconnecter()
    navigate('/connexion')
  }

  useEffect(() => {
    api
      .get(`/patient/paiements/${reference}`)
      .then(({ data }) => setPaiement(data))
      .catch((err) => {
        const code = err.response?.status
        if (code === 401 || code === 403) sessionExpiree()
        else if (code === 404) setErreur('Paiement introuvable')
        else setErreur('Impossible de joindre le serveur')
      })
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [reference])

  async function payer(succes) {
    setErreur('')
    setEnvoi(true)
    try {
      const { data } = await api.post(`/patient/paiements/${reference}/confirmer`, { succes })
      setPaiement(data)
    } catch (err) {
      const code = err.response?.status
      if (code === 401 || code === 403) sessionExpiree()
      else if (code === 409) setErreur('Ce paiement a déjà été traité ou a expiré')
      else setErreur('Impossible de joindre le serveur')
    } finally {
      setEnvoi(false)
    }
  }

  if (!paiement && !erreur) return <div className="page">Chargement...</div>

  return (
    <div className="page">
      <div className="formulaire">
        <h2>Paiement</h2>

        {erreur && <div className="alerte erreur">{erreur}</div>}

        {paiement && (
          <>
            <div className="wave-carte">
              <div className="wave-entete">Wave (simulation)</div>
              <div className="wave-corps">
                <div className="wave-montant">{paiement.montant.toLocaleString('fr-FR')} FCFA</div>
                <div>Frais de dossier : consultation vidéo</div>
                <div className="detail">{paiement.specialisteNom}</div>
                <div className="detail">{formater(paiement.debut)}</div>
                <div className="detail">Référence : {paiement.reference}</div>

                {paiement.statut === 'EN_ATTENTE' && (
                  <>
                    <div className="alerte attente">
                      Simulation : aucun argent n'est débité. Tu as 15 minutes pour payer.
                    </div>
                    <button className="bouton" disabled={envoi} onClick={() => payer(true)}>
                      Payer {paiement.montant.toLocaleString('fr-FR')} FCFA
                    </button>
                    <button
                      className="bouton secondaire"
                      disabled={envoi}
                      onClick={() => payer(false)}
                    >
                      Simuler un échec
                    </button>
                  </>
                )}
              </div>
            </div>

            {paiement.statut === 'PAYE' && (
              <>
                <div className="alerte ok">
                  Paiement réussi. Ton rendez-vous est confirmé.
                </div>
                <Link className="bouton" to="/rendez-vous">
                  Voir mes rendez-vous
                </Link>
              </>
            )}
            {paiement.statut === 'ECHOUE' && (
              <>
                <div className="alerte erreur">
                  Paiement échoué : le rendez-vous est annulé et le créneau est libéré.
                </div>
                <Link className="bouton secondaire" to="/">
                  Retour à la carte
                </Link>
              </>
            )}
            {paiement.statut === 'REMBOURSE' && (
              <div className="alerte attente">Ce paiement a été remboursé.</div>
            )}
          </>
        )}
      </div>
    </div>
  )
}
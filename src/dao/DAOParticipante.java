package dao;

import java.util.List;

import javax.persistence.NoResultException;
import javax.persistence.TypedQuery;

import modelo.Participante;

public class DAOParticipante extends DAO<Participante> {

	@Override
	public Participante read(Object chave) {
		try {
			String nome = (String) chave;
			TypedQuery<Participante> q = manager.createQuery("Select p from Participante p where p.nome=:n", Participante.class);
			q.setParameter("n", nome);
			return q.getSingleResult();
		} catch(NoResultException e) {
			return null;
		}
	}
	
	@Override
	public List<Participante> readAll(){
		TypedQuery<Participante> q = manager.createQuery("Select p from Participante p order by p.id", Participante.class);
		return q.getResultList();
	}

	public List<Participante> consultarParticipante(String nome, int mes){
		try {
			TypedQuery<Participante> q = manager.createQuery("select distinct p from Participante p join fetch p.reunioes r where substring(cast(r.datahora as text),6,2)= :m " + 
															"and p.nome= :n and Type(p) IN (Participante, Convidado)",Participante.class);
			//TypedQuery<Participante> q = manager.createQuery("select distinct p from Participante p join fetch p.reunioes r where substring(cast(r.datahora as text),6,2)= :m", Participante.class);
			q.setParameter("m", Integer.toString(mes));
			q.setParameter("n", nome);
			return q.getResultList();
		} catch(NoResultException e) {
			return null;
		}
	}
}



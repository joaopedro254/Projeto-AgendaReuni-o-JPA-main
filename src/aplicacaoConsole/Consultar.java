package aplicacaoConsole;
import fachada.Fachada;
import modelo.Participante;
import modelo.Reuniao;

public class Consultar {

	public Consultar() {
		try {
			Fachada.inicializar();
			Fachada.desabilitarEmail(true);

			System.out.println("\nQuais os participantes que tem reunião com participante P no mes m");
			for(Participante p : Fachada.consultaA("jose", 12))   //P=jose e M=12
				System.out.println(p);

			System.out.println("\nQuais as reuni�es que tem algum convidado");
			for(Reuniao r : Fachada.consultaB())
				System.out.println(r);

		} 
		catch (Exception e) {
			System.out.println("--->"+e.getMessage());
		}		
		Fachada.finalizar();
	}



	public static void main (String[] args) 
	{
		Consultar app = new Consultar();
	}
}



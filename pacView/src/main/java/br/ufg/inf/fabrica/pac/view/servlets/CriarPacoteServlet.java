/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package br.ufg.inf.fabrica.pac.view.servlets;

import br.ufg.inf.fabrica.pac.negocio.ICriarPacote;
import br.ufg.inf.fabrica.pac.negocio.dominio.Pacote;
import br.ufg.inf.fabrica.pac.negocio.dominio.Projeto;
import br.ufg.inf.fabrica.pac.negocio.dominio.Resposta;
import br.ufg.inf.fabrica.pac.negocio.dominio.Usuario;
import br.ufg.inf.fabrica.pac.negocio.imp.CriarPacote;
import br.ufg.inf.fabrica.pac.negocio.utils.FileService;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;

/**
 *
 * @author auf
 */
@WebServlet(name = "CriarPacoteServlet", urlPatterns = {"/CriarPacote"})
@MultipartConfig(
        fileSizeThreshold = 1024 * 1024, // 1MB
        maxFileSize = 1024 * 1024 * 10, // 10MB
        maxRequestSize = 1024 * 1024 * 10 // 10MB
)
public class CriarPacoteServlet extends HttpServlet {

    /**
     * Processes requests for both HTTP <code>GET</code> and <code>POST</code>
     * methods.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    private static final String BASE_DIR = "C:\\Users\\auf\\Desktop\\UploadsPAC";
    Resposta<Pacote> resposta = new Resposta<Pacote>();

    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException, ParseException {

        response.setContentType("text/html;charset=UTF-8");
        response.setCharacterEncoding("UTF-8");
        request.setCharacterEncoding("UTF-8");

//      %Indica resposta tanto para sucesso quanto para erros
        resposta.setChave(null);

        try {
            boolean ehNovoPacote = false;
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");

            Pacote pacote = new Pacote();
            pacote.setNome(request.getParameter("nomePacote"));
            pacote.setDescricao(request.getParameter("descricaoPacote"));
            if (request.getParameter("abandonado").equalsIgnoreCase("on") && request.getParameter("abandonado") != null) {
                pacote.setAbandonado(true);
            } else {
                pacote.setAbandonado(false);
            }
            if (request.getParameter("dataCriacao") == null) {
                ehNovoPacote = true;
            } else {
                pacote.setDataCriacao(sdf.parse(request.getParameter("dataCriacao")));
            }

            pacote.setDataPrevistaRealizacao(sdf.parse(request.getParameter("dataPrevistaRealizacao")));

            request = salvarDocumento(request, response);

            pacote.setDocumento(request.getAttribute("documento").toString());

            Usuario usuarioLogado = null;

            usuarioLogado = (Usuario) request.getSession().getAttribute("usuarioLogado");
            Projeto projetoSelecionado = new Projeto();
            projetoSelecionado.setId(1);
//            projetoSelecionado = (Projeto) request.getSession().getAttribute("projetoSelecionado");
            ICriarPacote cp = new CriarPacote();

            if (ehNovoPacote) {
                resposta = cp.criarPacote(pacote, usuarioLogado, projetoSelecionado);
                if (resposta.getChave() != null) {
                    request.setAttribute("resposta", resposta);
                    request.getRequestDispatcher("criarPacote.jsp").forward(request, response);
                } else {
                    request.setAttribute("resposta", resposta);
                    request.getRequestDispatcher("criarPacote.jsp").forward(request, response);
                }
            }
        } catch (IOException | ParseException | ServletException ex) {
            resposta.setChave(null);
            resposta.addItemLaudo("Falha na criação do pacote");
            request.getRequestDispatcher("criarPacote.jsp").forward(request, response);
            Logger.getLogger(CriarPacoteServlet.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    // <editor-fold defaultstate="collapsed" desc="HttpServlet methods. Click on the + sign on the left to edit the code.">
    /**
     * Handles the HTTP <code>GET</code> method.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            processRequest(request, response);
        } catch (ParseException ex) {
            Logger.getLogger(CriarPacoteServlet.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * Handles the HTTP <code>POST</code> method.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            processRequest(request, response);
        } catch (ParseException ex) {
            Logger.getLogger(CriarPacoteServlet.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * Returns a short description of the servlet.
     *
     * @return a String containing servlet description
     */
    @Override
    public String getServletInfo() {
        return "Short description";
    }// </editor-fold>

    private HttpServletRequest salvarDocumento(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        FileService service = new FileService();
        String nomeCompletoDocumento;
        Path destination = service.createFolder(BASE_DIR);

        for (Part part : request.getParts()) {
            if (part.getName().contains("documento") && Files.exists(destination)) {
                nomeCompletoDocumento = service.saveFile(destination, part);
                if (nomeCompletoDocumento != null) {
                    request.setAttribute("documento", nomeCompletoDocumento);
                }
            }
        }
        if (request.getAttribute("documento") == null) {
            resposta.addItemLaudo("Documento não pode ser enviado!");
            request.setAttribute("resposta", resposta);
            request.getRequestDispatcher("criarPacote.jsp").forward(request, response);
        }

        return request;
    }

}

package br.edu.ifc.bikes.service;

import br.edu.ifc.bikes.dto.RelatorioLocacaoDTO;
import br.edu.ifc.bikes.entity.Devolucao;
import br.edu.ifc.bikes.entity.Locacao;
import br.edu.ifc.bikes.repository.DevolucaoRepository;
import br.edu.ifc.bikes.repository.LocacaoRepository;
import lombok.RequiredArgsConstructor;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
@Service
public class RelatorioLocacaoService {

    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final LocacaoRepository locacaoRepository;
    private final DevolucaoRepository devolucaoRepository;

    @Transactional(readOnly = true)
    public byte[] gerarPdfRelatorioLocacoes() {
        List<RelatorioLocacaoDTO> dados = montarDados();
        try (InputStream template = new ClassPathResource(
                "reports/relatorio-locacoes.jrxml").getInputStream()) {
            var compiled = JasperCompileManager.compileReport(template);
            JasperPrint print = JasperFillManager.fillReport(
                    compiled,
                    new java.util.HashMap<>(),
                    new JRBeanCollectionDataSource(dados));
            return JasperExportManager.exportReportToPdf(print);
        } catch (Exception ex) {
            throw new IllegalStateException("Não foi possível gerar o relatório de locações", ex);
        }
    }

    @Transactional(readOnly = true)
    public List<RelatorioLocacaoDTO> listarDadosRelatorio() {
        return montarDados();
    }

    private List<RelatorioLocacaoDTO> montarDados() {
        List<RelatorioLocacaoDTO> dados = new ArrayList<>();
        for (Locacao locacao : locacaoRepository.findAll()) {
            LocalDateTime inicio = locacao.getDataHoraRetirada();
            LocalDateTime fim = locacao.getDataHoraEfetivaDevolucao();

            if (fim == null) {
                Devolucao devolucao = devolucaoRepository
                        .findByLocacaoId(locacao.getId())
                        .orElse(null);
                if (devolucao != null) {
                    fim = devolucao.getDataHoraEfetiva();
                }
            }

            dados.add(new RelatorioLocacaoDTO(
                    locacao.getCliente().getNome(),
                    locacao.getBicicleta().getCodigo(),
                    formatar(inicio),
                    formatar(fim),
                    calcularDuracao(inicio, fim),
                    locacao.getStatus().name()));
        }
        return dados;
    }

    private String calcularDuracao(LocalDateTime inicio, LocalDateTime fim) {
        if (inicio == null) {
            return "-";
        }
        LocalDateTime finalEfetivo = fim != null ? fim : LocalDateTime.now();
        long minutos = Math.max(0, Duration.between(inicio, finalEfetivo).toMinutes());
        long horas = minutos / 60;
        long restantes = minutos % 60;
        if (horas == 0) {
            return restantes + " min";
        }
        return horas + " h " + restantes + " min";
    }

    private String formatar(LocalDateTime valor) {
        return valor == null ? "-" : FORMATTER.format(valor);
    }
}

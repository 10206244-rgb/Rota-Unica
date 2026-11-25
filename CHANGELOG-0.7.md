# Rota Única 0.7 — revisão de pesquisa e navegação

## 50 alterações de código desta revisão

### Pesquisa
1. Normalização de acentos.
2. Correspondência sem distinguir maiúsculas de minúsculas.
3. Normalização de pontuação.
4. Reconhecimento de abreviações de tipos de logradouro.
5. Tolerância à ausência de artigos e preposições nos nomes.
6. Correspondência por prefixo de cada palavra.
7. Maior relevância para nomes completos iguais.
8. Priorização de nomes que começam com a busca.
9. Correspondência complementar pelo bairro e município.
10. Maior relevância para vias cadastradas como ruas.
11. Ordenação combinada por relevância e distância.
12. Pesquisa complementar na base oficial de arruamento de Serra.
13. Pesquisa pelo nome anterior do logradouro.
14. Pesquisa pelo nome oficial por extenso.
15. Exibição do CEP cadastrado na base municipal.
16. Posicionamento da via usando sua geometria cadastrada.
17. Restrição da consulta municipal à Grande Vitória.
18. Consulta municipal em executor independente.
19. Busca Photon com caixa geográfica local também na digitação.
20. Viés geográfico do Photon configurado.
21. Segunda busca sem limitar a caixa local quando necessário.
22. Tentativa alternativa sem prefixos e preposições.
23. Busca complementar de ruas nomeadas no OSM ao confirmar a pesquisa.
24. Consulta ao geocodificador Android como reserva na busca confirmada.
25. Reutilização de até 40 consultas recentes em memória.
26. Sugestões do histórico local de destinos.
27. Publicação incremental das respostas.
28. Identificador de consulta visível às threads para descartar respostas obsoletas.
29. Espera de digitação reduzida para 350 ms.
30. Até três tarefas de busca independentes.
31. Dois trabalhadores para a consulta municipal.
32. Limites de conexão e leitura menores na pesquisa.
33. Separação visual dos resultados.
34. Remoção de marcadores comerciais da pesquisa anterior ao escolher um destino.
35. Invalidação da rota anterior e de cálculos pendentes ao escolher outro destino.

### Navegação e alertas
36. Carrinho vetorial no lugar da seta do veículo.
37. Marcador de veículo maior.
38. Opção de orientação contínua pela bússola.
39. Persistência da preferência de orientação.
40. Resposta de câmera por sensor em intervalo menor.
41. Zoom de navegação ampliado e adaptado à velocidade.
42. Contorno e linha da rota reforçados.
43. Inclinação da câmera ajustada à velocidade.
44. Prioridade visual aos radares, lombadas, câmeras e registros históricos.
45. Avisos de possíveis equipamentos históricos com indicação explícita de falta de confirmação.
46. Antecedência ampliada de avisos de lombadas e videomonitoramento.

### Trânsito e estabilidade
47. Consultas de congestionamentos e ocorrências executadas separadamente em paralelo.
48. Deduplicação dos registros de trânsito e tratamento de velocidade em m/s como alternativa a km/h.
49. Validade baseada no horário de publicação e identificação explícita da falta de cobertura geográfica.
50. Apresentação de pontos de congestionamento reportados, e aviso distinto de velocidade baixa medida pelo GPS do próprio veículo.

## Verificação e limitações
- A lógica Java de normalização e classificação foi reproduzida e testada localmente em 12 casos, com sucesso. Os testes JUnit correspondentes estão no projeto.
- As camadas oficiais de GeoSerra foram identificadas; a resposta da consulta real pelo Android ainda precisa ser validada.
- A camada pública Waze Jams de Vitória não cobre toda a Serra. Não há integração com o feed global privado do Waze.
- Não há contagem regressiva real de semáforo sem fonte de fase e sincronização. O cronômetro mede a espera; o histórico local pode fornecer uma estimativa.
- O APK, a execução dos testes no CI e os testes em dispositivo ainda não foram confirmados.
- O mapa offline pesado permanece opcional. Não é necessário para o funcionamento normal do mapa online.

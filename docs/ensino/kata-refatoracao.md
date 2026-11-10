# Kata de refatoração: o controller legado

[← Material de ensino](README.md)

**Formato:** grupos de 3, 35 minutos. 10 min para achar os problemas, 20 para propor a versão
refatorada (no papel ou no projeto), 5 para apresentar.

## O código

```java
@RestController
@RequestMapping("/api/invoices")
public class InvoiceController {

    private static final Logger log = LoggerFactory.getLogger(InvoiceController.class);

    @Autowired private InvoiceService service;

    @GetMapping("/{id}")
    public ResponseEntity<Object> get(@PathVariable String id) {
        try {
            Invoice invoice = service.find(id);
            if (invoice == null) {
                return ResponseEntity.ok(Map.of("success", false, "error", "Invoice not found"));
            }
            return ResponseEntity.ok(invoice);
        } catch (Exception e) {
            log.error("Erro ao buscar invoice", e);
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping
    public ResponseEntity<Object> create(@RequestBody InvoiceRequest req) {
        if (req.getAmount() == null || req.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            return ResponseEntity.status(500).body("amount inválido");
        }
        try {
            return ResponseEntity.ok(service.create(req));
        } catch (TaxServiceException e) {
            return ResponseEntity.status(e.getStatusCode()).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(400).body("Erro: " + e);
        }
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable String id) {
        try {
            service.delete(id);
        } catch (Exception e) {
        }
    }
}

@Service
public class InvoiceService {
    @Transactional
    public Invoice create(InvoiceRequest req) throws TaxServiceException {
        Invoice invoice = repository.save(new Invoice(req));
        try {
            invoice.setTax(taxClient.calculate(invoice));            // HTTP dentro da transação
        } catch (IOException e) {
            log.error("falha no imposto", e);
            throw new TaxServiceException(503, "Tax service down: " + e.getMessage());
        }
        return invoice;
    }
}
```

## Gabarito: os problemas

<details><summary>Abrir lista (18 problemas)</summary>

**Controller**
1. try/catch em todo endpoint: formato diferente em cada um (P01).
2. "Não encontrado" como **200** com `success:false`.
3. `catch (Exception)` devolvendo `e.getMessage()`: vaza detalhe interno.
4. Log no controller **e** (no service) antes de relançar: log-and-rethrow, dois logs por erro.
5. Validação manual de `amount` em vez de Bean Validation (`@Valid` + `@Positive`).
6. Entrada inválida devolvendo **500**.
7. `catch (Exception)` no `create` devolvendo **400**: bug vira culpa do cliente.
8. `"Erro: " + e`: o `toString()` da exceção (com nome da classe) vai para o cliente.
9. Repassa o status do serviço de impostos (`e.getStatusCode()`) direto para o cliente.
10. `delete` engole a exceção: cliente recebe 200 mesmo quando falha.
11. Corpo de erro em texto puro num endpoint JSON.
12. `@Autowired` em campo (techpill DI.1): não é de erro, mas sempre aparece junto.

**Service**
13. `TaxServiceException` **checked** num método `@Transactional`: sem `rollbackFor`, a invoice
    salva **comita** mesmo com a falha.
14. Chamada HTTP **dentro** da transação: segura conexão do banco durante a latência externa.
15. Causa perdida: `new TaxServiceException(503, "..." + e.getMessage())` sem passar `e`.
16. Mensagem técnica do provedor (`e.getMessage()`) vai parar na resposta via controller.
17. Status HTTP (`503`) dentro de exceção de serviço: HTTP no domínio.
18. `find` retornando `null` em vez de `Optional`/exceção.
</details>

## Gabarito: a versão refatorada

<details><summary>Abrir</summary>

```java
@RestController
@RequestMapping("/api/invoices")
class InvoiceController {

    private final InvoiceService service;

    InvoiceController(InvoiceService service) { this.service = service; }

    @GetMapping("/{id}")
    InvoiceResponse get(@PathVariable UUID id) {
        return InvoiceResponse.from(service.get(id));            // NotFoundException → 404 no advice
    }

    @PostMapping
    ResponseEntity<InvoiceResponse> create(@Valid @RequestBody InvoiceRequest req) {   // @Positive amount → 400
        Invoice invoice = service.create(req);
        return ResponseEntity.created(URI.create("/api/invoices/" + invoice.id())).body(InvoiceResponse.from(invoice));
    }

    @DeleteMapping("/{id}")
    ResponseEntity<Void> delete(@PathVariable UUID id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}

@Service
class InvoiceService {

    public Invoice create(InvoiceRequest req) {
        Tax tax = taxGateway.calculate(req);                     // fora da transação; IntegrationException se falhar
        return transactionTemplate.execute(status -> repository.save(new Invoice(req, tax)));
    }

    public Invoice get(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new NotFoundException(InvoiceErrorCode.INVOICE_NOT_FOUND, id));
    }
}

// Adaptador: único lugar que conhece a exceção do cliente de impostos
class HttpTaxGateway implements TaxGateway {
    public Tax calculate(InvoiceRequest req) {
        try {
            return client.calculate(req);
        } catch (IOException e) {
            throw new IntegrationException(InvoiceErrorCode.TAX_SERVICE_UNAVAILABLE, e);   // causa preservada
        }
    }
}
```

O resto (status, formato, log, correlationId) é responsabilidade do advice global, que já existe.
O controller perdeu ~70% das linhas e ganhou um contrato consistente.
</details>

## Perguntas para fechar o kata

- Qual desses problemas vocês já viram em produção? Qual foi o impacto?
- Quais deles um teste de contrato teria pegado?
- Quais o checklist do [cap. 11](../11-checklist-e-anti-patterns.md) pegaria no code review?

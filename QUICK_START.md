# 🚀 Guia Rápido - Executar o Projeto Wallet

## Pré-requisitos Verificados ✓
- Java 21 instalado
- Maven 3.9.12 instalado
- Docker 29.2.1 instalado
- Docker Compose 1.29.2 instalado

## Passo 1: Iniciar Infraestrutura
Antes de rodar a aplicação, inicie PostgreSQL, Kafka e Redis:

**Terminal:**
```bash
cd src/main/resources/compose
docker-compose up -d
```

**Ou via Task do VS Code:**
- Abra a Paleta de Comandos: `Ctrl+Shift+P`
- Digite: `Tasks: Run Task`
- Selecione: `Docker: Start Services (PostgreSQL, Kafka, Redis)`

## Passo 2: Compilar o Projeto
**Terminal:**
```bash
mvn clean install
```

**Ou via Task do VS Code:**
- Paleta de Comandos: `Ctrl+Shift+P`
- Digite: `Tasks: Run Task`
- Selecione: `Maven: Build Project`

## Passo 3: Rodar Testes
**Terminal:**
```bash
mvn test
```

**Ou via Task do VS Code:**
- Paleta de Comandos: `Ctrl+Shift+P`
- Digite: `Tasks: Run Task`
- Selecione: `Maven: Run Tests`

## Passo 4: Executar a Aplicação
**Terminal:**
```bash
mvn spring-boot:run
```

**Ou via Task do VS Code:**
- Paleta de Comandos: `Ctrl+Shift+P`
- Digite: `Tasks: Run Task`
- Selecione: `Maven: Run Application`

## Acessar a Aplicação
- **API Base:** http://localhost:8080/wallet
- **Swagger UI:** http://localhost:8080/wallet/swagger-ui/index.html
- **Postman Collection:** `collections/Wallet.postman_collection.json`

## Parar Infraestrutura
```bash
cd src/main/resources/compose
docker-compose down
```

**Ou via Task do VS Code:**
- Paleta de Comandos: `Ctrl+Shift+P`
- Digite: `Tasks: Run Task`
- Selecione: `Docker: Stop Services`

## Atalhos Úteis

| Ação | Atalho |
|------|--------|
| Paleta de Comandos | `Ctrl+Shift+P` |
| Executar Task | `Ctrl+Shift+P` → "Tasks: Run Task" |
| Terminar Task Atual | `Ctrl+Shift+P` → "Tasks: Terminate Task" |
| Terminal Integrado | `Ctrl+Backtick` |

---

**Nota:** A configuração do VS Code (.vscode/tasks.json, launch.json e extensions.json) foi criada para facilitar a execução do projeto dentro do editor.

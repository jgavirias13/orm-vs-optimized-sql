# ORM vs Optimized SQL 🚀

**Why trusting your ORM blindly can kill performance – Spring Boot + PostgreSQL demo.**

This repository demonstrates how **naïve ORM queries** (Hibernate/JPA) can lead to severe performance issues, and how **optimized SQL queries** can drastically improve execution times.  

The goal is to show developers and architects why it’s essential to understand **how ORMs generate queries**, and to highlight the importance of **query optimization** when building scalable systems.

---

## 🔎 Context
In many enterprise systems, ORMs like Hibernate are used without considering the SQL they generate.  
This can cause:
- Unnecessary joins
- Cartesian products
- Inefficient filtering
- Missing indexes  

In real-world projects, I’ve seen operations go from **minutes down to milliseconds** just by rewriting queries and optimizing database access.

---

## 🛠️ Tech Stack
- **Java 17**
- **Spring Boot 3.x**
- **Hibernate (JPA)**
- **PostgreSQL**
- **Docker** (for DB setup)
- **JUnit** (for simple benchmarks)

---

## 📌 Scenarios
1. **Slow ORM query** – Using default JPA/Hibernate mapping.  
2. **Optimized native SQL** – Hand-crafted SQL + index.  

Each scenario will be benchmarked and compared.

---

## 🚀 How to Run
1. Clone this repository:
```bash
git clone https://github.com/your-username/orm-vs-optimized-sql.git
cd orm-vs-optimized-sql
```

2. Start PostgreSQL (Docker recommended):
```bash
docker run --name pg-perf -e POSTGRES_PASSWORD=postgres -e POSTGRES_DB=performance_demo -p 5432:5432 -d postgres:15-alpine
```

3. Update application.yml with your DB credentials.
4. Run the Spring Boot application:
```bash
./mvnw spring-boot:run
```

## 📊 Results (Coming Soon)
Benchmarks will compare:
	•	Execution times
	•	Query plans (EXPLAIN ANALYZE)
	•	Impact of indexes

Stay tuned for results and real metrics!

## 📖 Related Article
I’m writing a companion article:
👉 “Hibernate vs Native SQL: how we reduced queries from minutes to seconds”

## 🤝 Contributing
Feel free to open issues or suggest new scenarios (e.g., N+1 queries, missing indexes, etc.).

## 📌 License
MIT – Free to use for learning and demos.
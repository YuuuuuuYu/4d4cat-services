global:
  scrape_interval: 15s

scrape_configs:
  - job_name: 'monitoring-service'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets: ['monitoring:8080']
  - job_name: 'api-service'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets: ['${ORACLE_MAIN_IP}:8080', '${ORACLE_SUB_IP}:8080']
  - job_name: 'data-service'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets: ['${DATA_SERVER_IP}:8081']
locals {
  # Repo
  github = {
    org        = "pagopa"
    repository = "emd-citizen"
  }

  repo_env = var.env_short == "p" ? {
    SONARCLOUD_PROJECT_KEY = "pagopa_emd-citizen"
    SONARCLOUD_ORG         = "pagopa"
  } : {}

  repo_secrets = var.env_short == "p" ? {
    SONAR_TOKEN = data.azurerm_key_vault_secret.sonar_token[0].value
  } : {}
}

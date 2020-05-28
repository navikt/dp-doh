# 1 to NAIS in 60 minutes

## Instructions

### 1. Create basic service contract

```
apiVersion: "nais.io/v1alpha1"
kind: "Application"
metadata:
  name: nais-testapp
  namespace: default
  labels:
    team: aura
spec:
  image: navikt/nais-testapp:latest
```

The image must be tagged with something else than `latest`. If you try to deploy
things using `latest`, NAIS will only assume nothing has changed, and do
nothing.

The following steps are all optional!

### 2. Expose your service port

```
  port: 8080
```

This is the port your application is listening on. NAIS will by default expose
this as port 80. This can be overridden.

### 3. Configure your ingress

```
  ingresses:
    - "https://nais-testapp.dev-sbs.nais.io"
```

Each cluster has a wildcard DNS record like `*.<clusternavn>.nais.io` that can
be used.

[All clusters](https://github.com/nais/doc/tree/master/content/clusters) are currently:
- \*.dev-fss.nais.io
- \*.dev-sbs.nais.io
- \*.prod-fss.nais.io
- \*.prod-sbs.nais.io

### 4. Add health checks

```
  readiness:
    path: /isready
  liveness:
    path: /isalive
```

The readiness check is used to check when your application is ready to start
receiving traffic.

The liveness check is used to check if your application is still alive.

[Read more in the Kubernetes
docs](https://kubernetes.io/docs/tasks/configure-pod-container/configure-liveness-readiness-probes/)

### 5. Set up collection of metrics

```
  prometheus:
    path: /metrics
```

### 6. Configure desired number of instances

```
  replicas:
    min: 2
    max: 4
```

The Horizontal Pod Scaler will automatically adjust the number of pods based on
load.

### 7. Configure resource demands

```
  resources:
    requests:
      cpu: 200m
      memory: 256mi
    limits:
      cpu: 500m
      memory: 512mi
```

Requests is what you reserve for your app (multiplied by the number of
instances). You should try to keep this to a minimum. Start with a rough
estimate and use monitoring to tune it downwards.

Limits is the maximum your app is allowed to use. If it goes over the threshold
for a certain period of time, Kubernetes will kill that instance and start a new
one.

### 8. Set environment variables

```
  env:
    - name: MY_CUSTOM_VAR
      value: some_value
```

You can set some environment variables directly in nais.yaml. F.ex. which kind
of log format you want to use, or the default timeout for requests.

You can also set this in a [ConfigMap](https://github.com/nais/naiserator/blob/bae9741b55a77f038b9f3548f68616c0ce3f67cb/examples/nais-max.yaml#L49).

### 9. Create system user for authentication

Create an application in Fasit.
Order a system user in Basta.

### 10. Add secrets

Follow the [instructions for adding applications to
Vault](https://github.com/navikt/vault-iac/blob/master/doc/endusers.md) and
add your new application.

```
  vault:
    enabled: true
```

This will mount any secrets found in Vault as a volume in the pod. The
[baseimage will automatically source](https://github.com/navikt/baseimages/blob/b61adac56911d98515d8c60f3c13773d92f8ac52/java-common/init-scripts/02-import-env-files.sh#L5) all secrets that ends in `.env` as environment variables.

### 11. Expose your service

Inside the NAIS cluster, your application can be reached simply through the
application name. E.g. another service can call `http://nais-testapp/`.

Outside of the cluster, one of the Ingress URLs must be used.

The service is not registered in Fasit, and any external consumer must figure
out how to handle the URL for your application.

### 12. Deploy your application:

`kubectl apply -f nais.yaml`

## FAQ

Q: How do I get the URL to other services?
A: Services inside the cluster can be reached by using `https://app-name`. This
can either be hardcoded into your code, or controlled through
environment variables.

URLs to services outside of the cluster must be manually added under the
env-section, configmap, or through Vault.

Q: How can I support multiple environments?
A: Multiple yaml-files


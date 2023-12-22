#!/bin/bash

export ECR_REGISTRY=${ECR_REGISTRY}
export AWS_ECR_REPOSITORY=${AWS_ECR_REPOSITORY}
export IMAGE_TAG=${IMAGE_TAG}

deployment_name="notification-service"
service_name="notification-service"
pv_name="notification-ebs-pv"
pvc_name="notification-ebs-pvc"
namespace="prod"

# Update AWS EKS user config
aws eks update-kubeconfig --region ap-northeast-2 --name $AWS_EKS_CLUSTER_NAME

if kubectl get pv "${service_name}" -n "${namespace}" &> /dev/null; then
  echo "PersistenceVolume is already exists"
else
  kubectl create -f ./pv-prod.yml
fi

if kubectl get pvc "${service_name}" -n "${namespace}" &> /dev/null; then
  echo "PersistenceVolumeClaim is already exists"
else
  kubectl create -f ./pvc-prod.yml
fi

# Deploy kubernetes deployment resource
echo "Apply new kubernetes deployment resources..."
envsubst < ./deployment-prod.yml | kubectl apply -f - -n ${namespace}

# 현재 실행중인 Deployment의 Pod 이름들 가져오기
pod_names=$(kubectl get pods -l app="${deployment_name}" -n "${namespace}" --output=jsonpath='{.items[*].metadata.name}')
for pod_name in $pod_names; do
  attempt=0
  while [[ $attempt -lt 3 ]]; do
    readiness_probe_status=$(kubectl get pod "${deployment_name}" -n "${namespace}" --template='{{range .status.conditions}}{{if eq .type "Ready"}}{{.status}}{{end}}{{end}}')

    if [[ "${readiness_probe_status}" == "True" ]]; then
      echo "Readiness probe is healthy for pod ${deployment_name} in namespace prod."
      echo "Success to create new kubernetes deployment resources"
      break
    else
      echo "Readiness probe is not healthy for pod ${deployment_name} in namespace prod. Sleeping for 60 seconds..."
      sleep 60
      ((attempt++))
    fi
  done
done

# Check deployment is correctly deployed
readiness_probe_status=$(kubectl get pod "${deployment_name}" -n "${namespace}" --template='{{range .status.conditions}}{{if eq .type "Ready"}}{{.status}}{{end}}{{end}}')
if [[ "${readiness_probe_status}" == "True" ]]; then
  echo "Check service is already available..."
  if kubectl get service "${service_name}" -n "${namespace}" &> /dev/null; then
    echo "Service is already available"
    echo "Update new service resource..."
    kubectl apply -f ./service-prod.yml -n ${namespace}
  else
    echo "Service is not founded"
    echo "Create new service resource..."
    kubectl create -f ./service-prod.yml -n ${namespace}

    echo "Success to create new service resource..."
  fi
else
  echo "Failed to create new kubernetes deployment resources"
  echo "Delete new kubernetes deployment resources"
  # rollout
  kubectl rollout undo deployment ${deployment_name} -n ${namespace}
  exit 1
fi
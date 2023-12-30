#!/bin/bash

export ECR_REGISTRY=${ECR_REGISTRY}
export AWS_ECR_REPOSITORY=${AWS_ECR_REPOSITORY}
export IMAGE_TAG=${IMAGE_TAG}

deployment_name="notification-deployment"
service_name="notification-service"
namespace="prod"

# Update AWS EKS user config
aws eks update-kubeconfig --region ap-northeast-2 --name $AWS_EKS_CLUSTER_NAME

# Deploy kubernetes deployment resource
echo "Apply new kubernetes deployment resources..."
envsubst < ./deployment-prod.yml | kubectl apply -f - -n ${namespace}

# Wait for the deployment to be available
echo "Check new kubernetes deployment resources status..."
kubectl rollout status deployment/${deployment_name} -n ${namespace}

if [ $? -eq 0 ]; then
  echo "Successful deploy new kubernetes deployment resources..."

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
  echo "Deployment failed. Rolling back to the previous version..."
  kubectl rollout undo deployment/${deployment_name} -n ${namespace}

  # Wait for the rollback to be available
  kubectl rollout status deployment/${deployment_name} -n ${namespace}

  # Check if the rollback was successful
  if [ $? -eq 0 ]; then
    echo "Rollback successful."
    exit 0
  else
    echo "Rollback failed. Manual intervention required."
    exit 1
  fi
fi

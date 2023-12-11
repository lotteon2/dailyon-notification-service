#!/bin/bash

export ECR_REGISTRY=${ECR_REGISTRY}
export AWS_ECR_REPOSITORY=${AWS_ECR_REPOSITORY}
export IMAGE_TAG=${IMAGE_TAG}

# Update AWS EKS user config
aws eks update-kubeconfig --region ap-northeast-2 --name $AWS_EKS_CLUSTER_NAME

# Deploy kubernetes deployment resource
echo "Apply new kubernetes deployment resources..."
envsubst < ./deployment-prod.yml | kubectl apply -f - -n prod

# Check if deployment deploy is successful
if kubectl rollout status deployment/notification-deployment -n prod | grep -q "successfully rolled out"; then
  echo "Success to create new kubernetes deployment resources"

  kubectl annotate deployment/notification-deployment kubernetes.io/change-cause="Change image to ${IMAGE_TAG}"

  echo "Apply new kubernetes Service resources..."
  kubectl apply -f ./service-prod.yml -n prod
else
  echo "Failed to create new kubernetes deployment resources"
  echo "Rolling update has failed. Initiating rollback..."

  kubectl rollout undo deployment/notification-deployment -n prod
  if kubectl rollout status deployment/notification-deployment -n prod | grep -q "successfully rolled out"; then
    echo "Rollback was successful"

    kubectl annotate deployment/notification-deployment kubernetes.io/change-cause="Rollback to previous image"
  else
    echo "Rollback failed. Manual intervention required."
  fi

  exit 1
fi

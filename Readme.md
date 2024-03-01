# Distributed Banking System Overview

This distributed banking system represents a robust infrastructure that enables clients to interact with a banking network for credit requests and debt repayments. The architecture encompasses a leader node, several bank nodes, and client interfaces, facilitating seamless financial transactions across the network.

## System Features

- **Leader Node Management**: Central to the system, the leader node orchestrates credit distributions and mediates communication among bank nodes.
- **Bank Nodes Operations**: Bank nodes evaluate credit requests and track repayments, maintaining detailed financial records.
- **Client Transactions**: Clients engage with the system for credit services and debt settlement through direct communication with the leader node.

## Getting Started

### Prerequisites

Ensure you have Gradle installed to manage project dependencies and tasks.

### Launch Instructions

1. **Leader Node**: Initialize the leader node with:
   ```bash
   gradle leader
   ```
   
2. **Bank Nodes**: Activate bank nodes (minimum of two for network integrity) with:
   ```bash
   gradle node
   ```
   Or, to specify initial funds:
   ```bash
   gradle node -Pmoney=1000
   ```
   
3. **Client Interface**: Connect to the system as a client using:
   ```bash
   gradle client
   ```

## Network Communication

The system employs a specialized protocol to manage interactions between the leader, bank nodes, and clients. This protocol supports a variety of financial operations, ensuring secure and efficient data exchange throughout the network.

## Key Operations

- **Credit Requests**: Clients can apply for credits, which are processed by the bank nodes under the supervision of the leader node.
- **Debt Repayment**: The system allows for straightforward debt settlement, updating client accounts accordingly.

## Reliability and Consistency

### Error Handling

The system is designed to withstand node failures, ensuring no data loss and maintaining operational continuity. Persistent storage mechanisms safeguard transaction records and client data against system disruptions.

### Transaction Management

With support for concurrent client sessions, the system guarantees the accurate sequencing and execution of transactions, preserving the integrity of financial records and client accounts.

## Conclusion

This distributed banking system exemplifies a comprehensive approach to decentralized financial management, offering a scalable solution for credit and repayment services within a robust and fault-tolerant network infrastructure.
type ErrorBannerProps = {
  message: string;
};

export function ErrorBanner({ message }: ErrorBannerProps) {
  return (
    <div className="error-banner" role="alert">
      <strong>Falha na operacao.</strong>
      <span>{message}</span>
    </div>
  );
}
